package com.strandls.file.scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.strandls.file.util.AppUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.strandls.file.RabbitMqConnection;
import com.strandls.file.util.PropertyFileUtil;
import com.strandls.mail_utility.model.EnumModel.FIELDS;
import com.strandls.mail_utility.model.EnumModel.INFO_FIELDS;
import com.strandls.mail_utility.model.EnumModel.MAIL_TYPE;
import com.strandls.mail_utility.model.EnumModel.MY_UPLOADS_DELETE_MAIL;
import com.strandls.mail_utility.producer.RabbitMQProducer;
import com.strandls.mail_utility.util.JsonUtil;

public class QuartzJob implements Job {

	private static final Logger logger = LoggerFactory.getLogger(QuartzJob.class);

	private static final String DELIMITER = "@@@";
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	private static final String BASE_PATH;
	private static final long MAIL_THRESHOLD;
	private static final long DELETE_THRESHOLD;

	static {
		Properties props = PropertyFileUtil.fetchProperty("config.properties");
		BASE_PATH = props.getProperty("storage_dir") + File.separatorChar + AppUtil.BASE_FOLDERS.myUploads.getFolder();
		MAIL_THRESHOLD = Long.parseLong(props.getProperty("scheduler_mail_trigger"));
		DELETE_THRESHOLD = Long.parseLong(props.getProperty("scheduler_delete_trigger"));
	}

	@Inject
	SessionFactory sessionFactory;

	@Inject
	Channel channel;

	@Override
	public void execute(JobExecutionContext context) {
		Session session = sessionFactory.openSession();
		try {
			System.out.println("\n\n***** SCHEDULER STARTS *****\n\n");
			RabbitMQProducer producer = new RabbitMQProducer(channel);
			try (Stream<Path> stream = Files.list(Paths.get(BASE_PATH)).filter(Files::isDirectory)) {
				List<Path> paths = stream.collect(Collectors.toList());
				String[] userData;
				for (Path p : paths) {
					String folder = p.getFileName().toString();
					if (!isNumeric(folder)) {
						continue;
					}
					String user = getUserInfo(session, Long.parseLong(folder));
					if (user == null) {
						continue;
					}

					try (Stream<Path> fieldPath = Files.walk(Paths.get(BASE_PATH + File.separatorChar + folder))) {
						List<String> files = fieldPath.filter(Files::isRegularFile).filter(f -> {
							long noOfDays = getDifference(getFileCreationDate(f));
							return noOfDays >= MAIL_THRESHOLD;
						}).map(f -> {
							File tmp = f.toFile();
							long noOfDays = getDifference(getFileCreationDate(f));
							return String.join(DELIMITER, String.valueOf(noOfDays), tmp.getAbsolutePath());
						}).collect(Collectors.toList());
						boolean sendMail = files.stream()
								.anyMatch(f -> Long.parseLong(f.split(DELIMITER)[0]) == MAIL_THRESHOLD);

						if (sendMail) {
							userData = user.split(DELIMITER);
							Map<String, Object> data = new HashMap<>();
							data.put(FIELDS.TYPE.getAction(), MAIL_TYPE.MY_UPLOADS_DELETE_MAIL.getAction());
							data.put(FIELDS.TO.getAction(), new String[] { userData[0] });
							data.put(FIELDS.SUBSCRIPTION.getAction(), Boolean.valueOf(userData[2]));
							Map<String, Object> model = new HashMap<>();
							model.put(MY_UPLOADS_DELETE_MAIL.USERNAME.getAction(), userData[1]);
							model.put(MY_UPLOADS_DELETE_MAIL.FROM_DATE.getAction(), getFormattedDate(new Date(), -18));
							model.put(MY_UPLOADS_DELETE_MAIL.TO_DATE.getAction(), getFormattedDate(new Date(), 2));
							data.put(FIELDS.DATA.getAction(), JsonUtil.unflattenJSON(model));

							Map<String, Object> mailData = new HashMap<String, Object>();
							mailData.put(INFO_FIELDS.TYPE.getAction(), MAIL_TYPE.MY_UPLOADS_DELETE_MAIL.getAction());
							mailData.put(INFO_FIELDS.RECIPIENTS.getAction(), Arrays.asList(data));
							producer.produceMail(RabbitMqConnection.EXCHANGE, RabbitMqConnection.ROUTING_KEY, null,
									JsonUtil.mapToJSON(mailData));
						}
						files.forEach(file -> {
							String[] uri = file.split(DELIMITER);
							Long fileCreatedBefore = Long.parseLong(uri[0]);
							String filePath = uri[1];
							if (fileCreatedBefore >= DELETE_THRESHOLD) {
								Path path = Paths.get(filePath);
								try {
									Files.delete(path);
								} catch (IOException ei) {
									logger.error(ei.getMessage());
								}
							}
						});
					}

				}
			}

			System.out.println("\n\n***** SCHEDULER ENDS *****\n\n");
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		} finally {
			session.close();
		}
	}

	public static String getUserInfo(Session session, Long id) {
		String sql = "select email, username, send_notification from suser where id = ?";
		Object[] userData = (Object[]) session.createNativeQuery(sql).setParameter(1, id).getSingleResult();
		return userData != null && userData.length == 3
				? String.join(DELIMITER, userData[0].toString(), userData[1].toString(), userData[2].toString())
				: null;
	}

	public static LocalDate getFileCreationDate(Path f) {
		File tmp = f.toFile();
		BasicFileAttributes attributes = null;
		LocalDate creation = null;
		try {
			attributes = Files.readAttributes(Paths.get(tmp.toURI()), BasicFileAttributes.class);
			creation = Instant.ofEpochMilli(attributes.creationTime().toMillis()).atZone(ZoneId.systemDefault())
					.toLocalDate();
			return creation;
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return creation;

	}

	public static LocalDateTime getFileCreationDateTime(Path f) {
		File tmp = f.toFile();
		BasicFileAttributes attributes = null;
		LocalDateTime creation = null;
		try {
			attributes = Files.readAttributes(Paths.get(tmp.toURI()), BasicFileAttributes.class);
			creation = Instant.ofEpochMilli(attributes.creationTime().toMillis()).atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			return creation;
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return creation;
	}

	public static long getDifference(LocalDate date) {
		return ChronoUnit.DAYS.between(date, LocalDate.now());
	}

	public static long getDifferenceMinutes(LocalDateTime date) {
		return ChronoUnit.MINUTES.between(date, LocalDateTime.now());
	}

	public static String getFormattedDate(Date d, int offset) {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
		c.setTime(d);
		c.add(Calendar.DATE, offset);
		return dateFormatter.format(c.getTime());
	}

	public static boolean isNumeric(String folder) {
		if (folder == null || folder.isEmpty()) {
			return false;
		}
		try {
			Long.parseLong(folder);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

}
