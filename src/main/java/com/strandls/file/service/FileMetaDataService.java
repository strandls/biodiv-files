package com.strandls.file.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.strandls.file.dao.FileMetaDataDao;
import com.strandls.file.model.FileMetaData;

public class FileMetaDataService extends AbstractService<FileMetaData>{

	@Inject
	private ObjectMapper objectMappper;

	@Inject
	public FileMetaDataService(FileMetaDataDao dao) {
		super(dao);
	}

	public FileMetaData save(String jsonString) throws JsonParseException, JsonMappingException, IOException {
		FileMetaData fileMetaData = objectMappper.readValue(jsonString, FileMetaData.class);
		save(fileMetaData);
		return fileMetaData;
	}
}
