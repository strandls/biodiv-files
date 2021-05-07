package com.strandls.file.service;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ServiceModule extends AbstractModule{

	@Override
	protected void configure() {
		bind(UploadedMetaDataService.class).in(Scopes.SINGLETON);
		bind(FileAccessService.class).in(Scopes.SINGLETON);
		bind(FileUploadService.class).in(Scopes.SINGLETON);
		bind(FileDownloadService.class).in(Scopes.SINGLETON);
	}
}
