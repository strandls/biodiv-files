package com.strandls.file.model;

import java.util.Map;

public class MyUpload {
	
	private String hashKey;
	private String fileName;
	private String type;
	private String path;
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
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Map<String, Object> getO() {
		return o;
	}
	public void setO(Map<String, Object> o) {
		this.o = o;
	}

}
