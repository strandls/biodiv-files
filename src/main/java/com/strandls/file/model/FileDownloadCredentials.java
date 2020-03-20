/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.strandls.file.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author sethuraman
 */
@Entity
@Table(name = "file_download_credentials")
@XmlRootElement
public class FileDownloadCredentials implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Column(name = "access_key")
    private String accessKey;
    @Basic(optional = false)
    @NotNull
    @Column(name = "is_deleted")
    private Boolean isDeleted;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId")
    private List<FileDownloads> fileDownloadsList;

    public FileDownloadCredentials() {
    }

    public FileDownloadCredentials(Integer id) {
        this.id = id;
    }

    public FileDownloadCredentials(Integer id, String name, String accessKey, Boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.accessKey = accessKey;
        this.isDeleted = isDeleted;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	@XmlTransient
    public List<FileDownloads> getFileDownloadsList() {
        return fileDownloadsList;
    }

    public void setFileDownloadsList(List<FileDownloads> fileDownloadsList) {
        this.fileDownloadsList = fileDownloadsList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileDownloadCredentials)) {
            return false;
        }
        FileDownloadCredentials other = (FileDownloadCredentials) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.strandls.hibernateormdemo.model.FileDownloadCredentials[ id=" + id + " ]";
    }
    
}
