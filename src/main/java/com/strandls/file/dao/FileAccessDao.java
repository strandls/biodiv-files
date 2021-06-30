package com.strandls.file.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.file.model.FileDownloads;

public class FileAccessDao extends AbstractDao<FileDownloads, Long> {

	private static final Logger logger = LoggerFactory.getLogger(FileAccessDao.class);

	@Inject
	protected FileAccessDao(SessionFactory sessionFactory) {
		super(sessionFactory);
		// TODO Auto-generated constructor stub
	}

	@Override
	public FileDownloads findById(Long id) {
		Session session = sessionFactory.openSession();
		FileDownloads entity = null;
		try {
			entity = session.get(FileDownloads.class, id);
		} catch (Exception e) {
			 logger.error(e.getMessage());
		} finally {
			session.close();
		}

		return entity;
	}

}
