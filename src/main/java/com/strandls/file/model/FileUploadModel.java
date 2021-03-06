package com.strandls.file.model;

import java.util.Map;

public class FileUploadModel {

	private String hashKey;
	private String fileName;
	private boolean isUploaded;
	private String type;
	private String uri;
	private String error;
	private Map<String, Object> o;

	public String getHashKey() {
		return hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isUploaded() {
		return isUploaded;
	}

	public void setUploaded(boolean isUploaded) {
		this.isUploaded = isUploaded;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Map<String, Object> getO() {
		return o;
	}

	public void setO(Map<String, Object> o) {
		this.o = o;
	}

}
