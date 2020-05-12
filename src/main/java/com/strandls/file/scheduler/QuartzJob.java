package com.strandls.file.scheduler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.strandls.file.RabbitMqConnection;
import com.strandls.mail_utility.model.EnumModel.FIELDS;
import com.strandls.mail_utility.model.EnumModel.MAIL_TYPE;
import com.strandls.mail_utility.model.EnumModel.MY_UPLOADS_DELETE_MAIL;
import com.strandls.mail_utility.producer.RabbitMQProducer;
import com.strandls.mail_utility.util.JsonUtil;

public class QuartzJob implements Job {
	
	@Inject
	SessionFactory sessionFactory;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			RabbitMqConnection connection = new RabbitMqConnection();
			Channel channel = connection.setRabbitMQConnetion();
			RabbitMQProducer producer = new RabbitMQProducer(channel);
			List<Path> paths = Files.list(Paths.get(BASE_PATH)).filter(Files::isDirectory).collect(Collectors.toList());
			String[] userData;
			System.out.println("Paths: " + paths);
			for (Path p : paths) {
				String folder = p.getFileName().toString();
				String user = getUserInfo(Long.parseLong(folder));
				if (user == null || user.contains("@ibp.org")) {
					continue;
				}
				List<String> files = Files.walk(Paths.get(BASE_PATH + File.separatorChar + folder))
						.filter(Files::isRegularFile).filter(f -> {
							long noOfDays = getDifference(getFileCreationDate(f));
							if (noOfDays >= MAIL_THRESHOLD) {
								return true;
							}
							return false;
						}).map(f -> {
							File tmp = f.toFile();
							long noOfDays = getDifference(getFileCreationDate(f));
							return String.join(DELIMITER, String.valueOf(noOfDays), tmp.getAbsolutePath());
						}).collect(Collectors.toList());
				System.out.println("Files: " + files);
				boolean sendMail = files.stream().filter(f -> Long.parseLong(f.split(DELIMITER)[0]) >= MAIL_THRESHOLD)
						.findAny().isPresent();
				System.out.println("Mail? " + sendMail);

				if (sendMail) {
					userData = user.split(DELIMITER);
					Map<String, Object> data = new HashMap<>();
					data.put(FIELDS.TYPE.getAction(), MAIL_TYPE.MY_UPLOADS_DELETE_MAIL.getAction());
					data.put(FIELDS.TO.getAction(), new String[] { userData[0] });
					Map<String, Object> model = new HashMap<>();
					model.put(MY_UPLOADS_DELETE_MAIL.USERNAME.getAction(), userData[1]);
					model.put(MY_UPLOADS_DELETE_MAIL.FROM_DATE.getAction(), getFormattedDate(new Date(), -18));
					model.put(MY_UPLOADS_DELETE_MAIL.TO_DATE.getAction(), getFormattedDate(new Date(), 2));
					data.put(FIELDS.DATA.getAction(), JsonUtil.unflattenJSON(model));
					producer.produceMail(RabbitMqConnection.EXCHANGE, RabbitMqConnection.ROUTING_KEY, null,
							JsonUtil.mapToJSON(data));

					data.put(FIELDS.TO.getAction(), new String[] { "sethu10121994@gmail.com", "thomas.vee@gmail.com" });
					producer.produceMail(RabbitMqConnection.EXCHANGE, RabbitMqConnection.ROUTING_KEY, null,
							JsonUtil.mapToJSON(data));
					System.out.println("Mail Sent");
				}

				files.forEach(file -> {
					String[] uri = file.split(DELIMITER);
					if (Integer.parseInt(uri[0]) >= DELETE_THRESHOLD) {
						File f = new File(uri[1]);
						f.delete();
						f.getParentFile().delete();
					}
				});
			}

			channel.getConnection().close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (session.isOpen()) {
				session.close();
			}
		}
	}

}
