package com.strandls.file.api;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class APIModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FileUploadApi.class).in(Scopes.SINGLETON);
		bind(FileDownloadApi.class).in(Scopes.SINGLETON);
		bind(FileDownloadOthers.class).in(Scopes.SINGLETON);
	}
}
