package com.strandls.file.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DaoModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FileMetaDataDao.class).in(Scopes.SINGLETON);
		bind(FileAccessDao.class).in(Scopes.SINGLETON);
	}
}
