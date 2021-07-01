package com.strandls.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.tika.io.FilenameUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.inject.Inject;

import com.strandls.file.dao.FileAccessDao;
import com.strandls.file.model.FileDownloadCredentials;
import com.strandls.file.model.FileDownloads;
import com.strandls.file.util.AppUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileAccessService {

	private static final Logger logger = LoggerFactory.getLogger(FileAccessService.class);

	String storageBasePath = null;

	public FileAccessService() {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv");
	}

	@Inject
	private SessionFactory factory;

	@Inject
	private FileAccessDao fileAccessDao;

	@SuppressWarnings({ "unchecked" })
	public FileDownloadCredentials getCredentials(String accessKey) {
		Session session = factory.openSession();
		FileDownloadCredentials credentials = null;
		String sql = "from FileDownloadCredentials f where f.accessKey = :key";
		try {
			Query<FileDownloadCredentials> query = session.createQuery(sql);
			query.setParameter("key", accessKey);
			credentials = query.getSingleResult();
		} catch (Exception e) {
			logger.error(e.getMessage());

		} finally {
			session.close();
		}
		return credentials;
	}

	private FileDownloads saveDownload(FileDownloadCredentials credentials, String fileName) {
		FileDownloads download = new FileDownloads();
		try {
			download.setUserId(credentials);
			download.setDate(new Date());
			download.setFileName(fileName);
			download = fileAccessDao.save(download);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return download;
	}

	public Response downloadFile(FileDownloadCredentials credentials) throws IOException {
		String dirPath = storageBasePath + File.separatorChar + "data-archive" + File.separatorChar + "gbif"
				+ File.separatorChar;
		Path path = Paths.get(dirPath);
		Optional<Path> file;
		try (Stream<Path> fileList = Files.list(path)) {
			file = fileList.filter(f -> !Files.isDirectory(f))
					.max(Comparator.comparingLong(f -> f.toFile().lastModified()));
		}
		if (!file.isPresent()) {
			throw new FileNotFoundException("Folder is empty");
		}
		File inputFile = file.get().toFile();
		saveDownload(credentials, inputFile.getName());
		InputStream in = new FileInputStream(inputFile);
		String contentType = URLConnection.guessContentTypeFromStream(in);
		StreamingOutput sout;
		sout = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				byte[] buf = new byte[8192];
				int c;
				while ((c = in.read(buf, 0, buf.length)) > 0) {
					output.write(buf, 0, c);
					output.flush();
				}
				in.close();
				output.close();
			}
		};
		return Response.ok(sout).type(contentType)
				.header("Content-Disposition", "attachment; filename=\"" + inputFile.getName() + "\"")
				.cacheControl(AppUtil.getCacheControl()).build();
	}

	public Response genericFileDownload(String filePath) throws IOException {
		String path = FilenameUtils.normalize(filePath);
		File inputFile = new File(path);
		InputStream in = new FileInputStream(path);
		String contentType = URLConnection.guessContentTypeFromStream(in);
		StreamingOutput sout;
		sout = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				byte[] buf = new byte[8192];
				int c;
				while ((c = in.read(buf, 0, buf.length)) > 0) {
					output.write(buf, 0, c);
					output.flush();
				}
				in.close();
				output.close();
			}
		};
		return Response.ok(sout).type(contentType)
				.header("Content-Disposition", "attachment; filename=\"" + inputFile.getName() + "\"")
				.cacheControl(AppUtil.getCacheControl()).build();
	}
}
