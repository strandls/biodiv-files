package com.strandls.file.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

import com.strandls.file.model.FileMetaData;

public class FileMetaDataDao extends AbstractDao<FileMetaData, Long> {

	@Inject
	public FileMetaDataDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public FileMetaData findById(Long id) {
		Session session = sessionFactory.openSession();
		FileMetaData entity = null;
		try {
			entity = session.get(FileMetaData.class, id);
		} catch (Exception e) {
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

}
