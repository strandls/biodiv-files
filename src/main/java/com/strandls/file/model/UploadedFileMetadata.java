package com.strandls.file.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "uploaded_file_metadata")
public class UploadedFileMetadata implements Serializable {

    public UploadedFileMetadata(Long userId, String uploadedFileOriginalName, String uploadedFileRenamed, String type) {
        this.userId = userId;
        this.uploadedFileOriginalName = uploadedFileOriginalName;
        this.uploadedFileRenamed = uploadedFileRenamed;
        this.type = type;
    }

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "uploaded_file_original_name", nullable = false)
    private String uploadedFileOriginalName;

    @Column(name = "uploaded_file_renamed", nullable = false)
    private String uploadedFileRenamed;

    @Column(name = "type", nullable = false)
    private String type;

}
