package com.strandls.file.service;

import com.strandls.file.dao.UploadedMetaDataDao;
import com.strandls.file.model.UploadedFileMetadata;

import javax.inject.Inject;
import java.util.Date;

public class UploadedMetaDataService extends AbstractService<UploadedFileMetadata> {

    @Inject
    public UploadedMetaDataService(UploadedMetaDataDao dao) {
        super(dao);
    }

    public UploadedFileMetadata saveUploadedFileMetadata(Long userId, String oldName, String newName, String type) {
        UploadedFileMetadata uploadedFileMetadata = new UploadedFileMetadata(userId, oldName, newName, type, new Date());
        save(uploadedFileMetadata);
        return uploadedFileMetadata;
    }

}
