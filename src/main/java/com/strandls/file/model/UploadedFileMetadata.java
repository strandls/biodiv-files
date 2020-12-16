package com.strandls.file.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "uploaded_file_metadata")
public class UploadedFileMetadata implements Serializable {

    public UploadedFileMetadata(Long userId, String uploadedFileOriginalName, String uploadedFileRenamed, String type, Date movementDate) {
        this.userId = userId;
        this.fileOriginalName = uploadedFileOriginalName;
        this.fileNewName = uploadedFileRenamed;
        this.type = type;
        this.movementDate = movementDate;
    }

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_original_name", nullable = false)
    private String fileOriginalName;

    @Column(name = "file_new_name", nullable = false)
    private String fileNewName;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "movement_date", nullable = false)
    private Date movementDate;

}
