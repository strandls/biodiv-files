package com.strandls.file.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.google.inject.Inject;
import com.strandls.file.model.FileDownloads;

public class FileAccessDao extends AbstractDao<FileDownloads, Long> {

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
			e.printStackTrace();
		} finally {
			session.close();
		}

		return entity;
	}

}
