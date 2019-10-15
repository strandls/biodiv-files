package com.strandls.file.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@Entity
@Table(name="file_meta_data")
@XmlRootElement
@ApiModel("File")
@JsonIgnoreProperties
public class FileMetaData implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7773327567662069379L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_id_generator")
	@SequenceGenerator(name = "file_id_generator", sequenceName = "file_id_seq", allocationSize = 1)
	@Column(name = "id", updatable = false, nullable = false)
	private long id;
	
	@Column(name = "path", updatable = false, nullable = false)
	private String path;
	
	@Column(name = "file_name", updatable = false, nullable = false)
	private String fileName;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
