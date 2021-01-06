package com.strandls.file.service;

import com.google.common.io.Files;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.model.MyUpload;
import com.strandls.file.model.comparator.UploadDateSort;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.AppUtil.BASE_FOLDERS;
import com.strandls.file.util.AppUtil.MODULE;
import com.strandls.file.util.ThumbnailUtil;
import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FileUploadService {
    private final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    String storageBasePath;
    SimpleDateFormat sdf;
    @Inject
    private UploadedMetaDataService uploadedMetaDataService;

    public FileUploadService() {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

        storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv-image");
    }

    /**
     * Resource Upload
     */
    public FileUploadModel uploadFile(BASE_FOLDERS directory, InputStream inputStream,
                                      FormDataContentDisposition fileDetails, String nestedFolder, String hashKey,
                                      boolean resourceFolder) throws IOException {

        FileUploadModel fileUploadModel = new FileUploadModel();

        String fileName = fileDetails.getFileName();

        String fileExtension = Files.getFileExtension(fileName);

        String folderName = "";
        if (nestedFolder != null && !nestedFolder.isEmpty()) {
            folderName += String.join(String.valueOf(File.separatorChar), nestedFolder.split(",")) + File.separatorChar;
        }
        folderName += "".equals(hashKey) ? UUID.randomUUID().toString() : hashKey;
        if (resourceFolder) {
            folderName += File.separatorChar + "resources";
        }
        String dirPath = storageBasePath + File.separatorChar + directory.getFolder() + File.separatorChar + folderName;
        Tika tika = new Tika();
        String probeContentType = tika.detect(fileName);

        if (probeContentType == null || !probeContentType.startsWith("image")) {
            fileUploadModel.setError("Invalid file type. Only image type allowed.");
            return fileUploadModel;
        } else {
            fileUploadModel.setType(probeContentType);
        }

        String tempFileName = UUID.randomUUID().toString().replaceAll("-", "");
        String generatedFileName = tempFileName + "." + fileExtension;

        String filePath = dirPath + File.separatorChar + generatedFileName;
        System.out.println("\n\n FileLocation: " + filePath + " *****\n\n");
        File file = new File(filePath);
        if (!file.getCanonicalPath().startsWith(storageBasePath)) {
            throw new IOException("Invalid folder");
        }
        boolean uploaded = writeToFile(inputStream, filePath);

        fileUploadModel.setUploaded(uploaded);

        if (probeContentType.startsWith("image")) {
            Thread thread = new Thread(new ThumbnailUtil(filePath, dirPath, tempFileName, fileExtension));
            thread.start();
        }

        if (uploaded) {
            String resultPath = File.separatorChar + folderName + File.separatorChar + generatedFileName;
            fileUploadModel.setHashKey(folderName);
            fileUploadModel.setFileName(generatedFileName);
            fileUploadModel.setUri(resultPath);
            return fileUploadModel;
        } else {
            fileUploadModel.setError("Unable to upload image");
            return fileUploadModel;
        }
    }

    private FileUploadModel uploadFile(String source, String directory, String hashKey, String fileName)
            throws IOException {

        FileUploadModel fileUploadModel = new FileUploadModel();

        String fileExtension = Files.getFileExtension(fileName);

        String folderName = "".equals(hashKey) ? UUID.randomUUID().toString() : hashKey;
        String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar + folderName;

        Tika tika = new Tika();
        String probeContentType = tika.detect(fileName);

        if (probeContentType == null || !probeContentType.startsWith("image") && !probeContentType.startsWith("audio")
                && !probeContentType.startsWith("video")) {
            fileUploadModel.setError("Invalid file type. Allowed types are image, audio and video");
            return fileUploadModel;
        } else {
            fileUploadModel.setType(probeContentType);
        }

        if ("".equals(hashKey)) {
            File dir = new File(dirPath);
            boolean created = dir.mkdir();
            if (!created) {
                fileUploadModel.setError("Directory creation failed");
                return fileUploadModel;
            }
        }

        String tempFileName = UUID.randomUUID().toString().replaceAll("-", "");
        String generatedFileName = tempFileName + "." + fileExtension;

        String filePath = dirPath + File.separatorChar + generatedFileName;
        File destFile = new File(filePath);
        boolean isFolderCreated = false;
        if (!destFile.getParentFile().exists()) {
            isFolderCreated = destFile.getParentFile().mkdirs();
        }
        if (!isFolderCreated) {
            fileUploadModel.setError("Directory creation failed");
            return fileUploadModel;
        }
        System.out.println("\n\n***** Source: " + source + " Destination: " + filePath + " *****\n\n");
        Path path = java.nio.file.Files.move(Paths.get(source), Paths.get(filePath), StandardCopyOption.ATOMIC_MOVE);
        boolean uploaded = path != null;

        fileUploadModel.setUploaded(uploaded);

        if (probeContentType.startsWith("image")) {
            Thread thread = new Thread(new ThumbnailUtil(filePath, dirPath, tempFileName, fileExtension));
            thread.start();
        }

        if (uploaded) {
            String resultPath = File.separatorChar + folderName + File.separatorChar + generatedFileName;
            fileUploadModel.setHashKey(folderName);
            fileUploadModel.setFileName(generatedFileName);
            fileUploadModel.setUri(resultPath);
            return fileUploadModel;
        } else {
            fileUploadModel.setError("Unable to upload image");
            return fileUploadModel;
        }
    }

    /**
     * Upload File to My-Uploads
     */
    public MyUpload saveFile(InputStream is, MODULE module, ContentDisposition contentDisposition, String hash,
                             Long userId) throws Exception {
        String dir = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.getFolder() + File.separatorChar
                + userId + File.separatorChar + hash;
        File dirFile = new File(dir);
        boolean dirCreated = false;
        if (!dirFile.exists()) {
            dirCreated = dirFile.mkdirs();
        }
        if (!dirCreated) logger.error("Directory creation failed");
        Tika tika = new Tika();
        String fileName = dir + File.separatorChar + contentDisposition.getFileName();
        File file = new File(fileName);
        if (file.getCanonicalPath().startsWith(dir) && file.getCanonicalFile().exists()) {
            return getExistingFileData(file);
        }
        String probeContentType = tika.detect(fileName);
        boolean allowedContentType = AppUtil.filterFileTypeForModule(probeContentType, module);
        if (!allowedContentType) {
            throw new Exception("Invalid file type. Allowed types are "
                    + String.join(", ", AppUtil.ALLOWED_CONTENT_TYPES.get(module)));
        }
        boolean isFileCreated = writeToFile(is, file.getAbsolutePath());
        MyUpload uploadModel = new MyUpload();
        if (isFileCreated) {
            uploadModel.setFileName(file.getName());
            uploadModel.setHashKey(hash);
            uploadModel
                    .setPath(File.separatorChar + file.getParentFile().getName() + File.separatorChar + file.getName());
            uploadModel.setType(probeContentType);
            BasicFileAttributes attributes;
            if (probeContentType.startsWith("image")) {
                String exifData = AppUtil.getExifData(file.getAbsolutePath());
                String[] data = exifData.split("\\*");
                if (!exifData.isEmpty() && exifData.contains("*")) {
                    int dataLength = data.length;
                    getFileAttributes(uploadModel, data, dataLength);
                }
            }
            attributes = java.nio.file.Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class);
            Date uploadedDate = new Date(attributes.creationTime().toMillis());
            uploadModel.setDateUploaded(uploadedDate);
            uploadModel.setFileSize(String.valueOf(file.length()));
        } else {
            throw new Exception("File not created");
        }
        return uploadModel;
    }

    /**
     * List Files in My-Uploads
     */
    public List<MyUpload> getFilesFromUploads(Long userId, MODULE module) {
        List<MyUpload> files = new ArrayList<>();
        String userDir = BASE_FOLDERS.myUploads.getFolder() + File.separatorChar + userId;
        try {
            Tika tika = new Tika();
            List<MyUpload> filesList = java.nio.file.Files
                    .find(java.nio.file.Paths.get(storageBasePath + File.separatorChar + userDir),
                            Integer.MAX_VALUE,
                            (p, bfa) -> {
                                String type = tika.detect(p.getFileName().toString());
                                return java.nio.file.Files.isRegularFile(p) && AppUtil.filterFileTypeForModule(type, module);
                            })
                    .map(f -> {
                        File tmpFile = f.toFile();
                        String probeContentType = tika.detect(tmpFile.getName());
                        MyUpload uploadModel = new MyUpload();
                        uploadModel.setHashKey(tmpFile.getParentFile().getName());
                        uploadModel.setFileName(tmpFile.getName());
                        BasicFileAttributes attributes;
                        if (probeContentType.startsWith("image")) {
                            String exifData = AppUtil.getExifData(tmpFile.getAbsolutePath());
                            if (!exifData.isEmpty() && exifData.contains("*")) {
                                String[] data = exifData.split("\\*");
                                int dataLength = data.length;
                                if (dataLength == 1) {
                                    String dateStr = data[0];
                                    Date capturedDate = null;
                                    try {
                                        if (!dateStr.isEmpty()) {
                                            attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()),
                                                    BasicFileAttributes.class);
                                            Date uploadedDate = new Date(attributes.creationTime().toMillis());
                                            uploadModel.setDateUploaded(uploadedDate);
                                            capturedDate = sdf.parse(dateStr);
                                        }
                                        uploadModel.setDateCreated(capturedDate);
                                    } catch (Exception ex) {
                                        logger.debug(ex.getMessage());
                                    }
                                } else if (dataLength == 2) {
                                    uploadModel.setLatitude(AppUtil.calculateValues(data[0]));
                                    uploadModel.setLongitude(AppUtil.calculateValues(data[1]));
                                } else if (dataLength == 3) {
                                    uploadModel.setLatitude(AppUtil.calculateValues(data[0]));
                                    uploadModel.setLongitude(AppUtil.calculateValues(data[1]));
                                    String dateStr = data[2];
                                    Date capturedDate = null;
                                    try {
                                        if (!dateStr.isEmpty()) {
                                            capturedDate = sdf.parse(dateStr);
                                        }
                                        uploadModel.setDateCreated(capturedDate);
                                    } catch (Exception ex) {
                                        logger.debug(ex.getMessage());
                                    }
                                }
                            }
                        }
                        Date uploadedDate;
                        try {
                            attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()),
                                    BasicFileAttributes.class);
                            uploadedDate = new Date(attributes.creationTime().toMillis());
                            uploadModel.setDateUploaded(uploadedDate);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        uploadModel.setPath(File.separatorChar + tmpFile.getParentFile().getName() + File.separatorChar
                                + tmpFile.getName());
                        uploadModel.setType(probeContentType);
                        uploadModel.setFileSize(String.valueOf(tmpFile.length()));
                        return uploadModel;
                    }).collect(Collectors.toList());
            files.addAll(filesList);
            files.sort(new UploadDateSort());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return files;
    }

    private MyUpload getExistingFileData(File tmpFile) throws Exception {
        Tika tika = new Tika();
        String probeContentType = tika.detect(tmpFile.getName());
        MyUpload uploadModel = new MyUpload();
        uploadModel.setHashKey(tmpFile.getParentFile().getName());
        uploadModel.setFileName(tmpFile.getName());
        BasicFileAttributes attributes;
        if (probeContentType.startsWith("image")) {
            String exifData = AppUtil.getExifData(tmpFile.getAbsolutePath());
            if (!exifData.isEmpty() && exifData.contains("*")) {
                String[] data = exifData.split("\\*");
                int dataLength = data.length;
                getFileAttributes(uploadModel, data, dataLength);
            }
            attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()), BasicFileAttributes.class);
            Date uploadedDate = new Date(attributes.creationTime().toMillis());
            uploadModel.setDateUploaded(uploadedDate);
        }
        attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()), BasicFileAttributes.class);
        Date uploadedDate = new Date(attributes.creationTime().toMillis());
        uploadModel.setDateUploaded(uploadedDate);
        uploadModel.setPath(
                File.separatorChar + tmpFile.getParentFile().getName() + File.separatorChar + tmpFile.getName());
        uploadModel.setType(probeContentType);
        uploadModel.setFileSize(String.valueOf(tmpFile.length()));
        return uploadModel;
    }

    private void getFileAttributes(MyUpload uploadModel, String[] data, int dataLength) {
        if (dataLength == 1) {
            String dateStr = data[0];
            Date capturedDate = null;
            try {
                if (!dateStr.isEmpty()) {
                    capturedDate = sdf.parse(dateStr);
                }
                uploadModel.setDateCreated(capturedDate);
            } catch (Exception ex) {
                logger.debug(ex.getMessage());
            }
        } else if (dataLength == 2) {
            uploadModel.setLatitude(AppUtil.calculateValues(data[0]));
            uploadModel.setLongitude(AppUtil.calculateValues(data[1]));
        } else if (dataLength == 3) {
            uploadModel.setLatitude(AppUtil.calculateValues(data[0]));
            uploadModel.setLongitude(AppUtil.calculateValues(data[1]));
            String dateStr = data[2];
            Date capturedDate = null;
            try {
                if (!dateStr.isEmpty()) {
                    capturedDate = sdf.parse(dateStr);
                }
                uploadModel.setDateCreated(capturedDate);
            } catch (Exception ex) {
                logger.debug(ex.getMessage());
            }
        }
    }

    /**
     * Delete Files from My-Uploads
     */
    public boolean deleteFilesFromMyUploads(Long userId, String fileName) throws IOException {
        boolean isDeleted = false;
        String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.getFolder() + File.separatorChar
                + userId;
        File f = new File(basePath + File.separatorChar + fileName);
        if (f.exists() && java.nio.file.Files.isRegularFile(Paths.get(f.toURI()))
                && f.getCanonicalPath().startsWith(basePath)) {
            isDeleted = f.delete() && f.getParentFile().delete();
        }
        return isDeleted;
    }

    /**
     * Move Files from My-Uploads
     */
    public Map<String, Object> moveFilesFromUploads(Long userId, List<String> fileList, String folderStr)
            throws Exception {
        Map<String, Object> finalPaths = new HashMap<>();
        BASE_FOLDERS folder = AppUtil.getFolder(folderStr);
        if (folder == null) {
            throw new Exception("Invalid folder");
        }
        try {
            String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.getFolder()
                    + File.separatorChar + userId;
            String folderBasePath = storageBasePath + File.separatorChar + folder.getFolder()
                    + File.separatorChar + userId;
            String hash = UUID.randomUUID().toString();
            String existingHash = fileList.stream().filter(path -> !path.startsWith(File.separatorChar + "ibpmu-"))
                    .findAny().orElse(null);
            if (existingHash != null && !existingHash.isEmpty()) {
                existingHash = existingHash.substring(1, existingHash.lastIndexOf(File.separatorChar));
            }
            Tika tika = new Tika();

            for (String file : fileList) {
                File folderFile = new File(folderBasePath + file);
                if (file.startsWith(File.separatorChar + "ibpmu-")) {
                    File f = new File(basePath + file);
                    if (f.exists()) {
                        String fileSize = String.valueOf(java.nio.file.Files.size(f.toPath()));
                        String fileName = f.getName();
                        FileUploadModel model = uploadFile(f.getAbsolutePath(), folder.getFolder(),
                                existingHash == null ? hash : existingHash, fileName);
                        String uri = model.getUri();
                        uri = uri.substring(uri.lastIndexOf(File.separatorChar) + 1);
                        uploadedMetaDataService.saveUploadedFileMetadata(userId, fileName,
                                uri, AppUtil.FILE_UPLOAD_TYPES.MOVE.toString());
                        Map<String, String> fileAttributes = new HashMap<>();
                        fileAttributes.put("name", model.getUri());
                        fileAttributes.put("mimeType", tika.detect(fileName));
                        fileAttributes.put("size", fileSize);
                        finalPaths.put(file, fileAttributes);
                        boolean isDeleted = f.getParentFile().delete();
                        if (!isDeleted) logger.debug("Could not delete directory: {}", f.getParentFile().getName());
                    }
                } else if (folderFile.exists()) {
                    String folderFileSize = String.valueOf(java.nio.file.Files.size(folderFile.toPath()));
                    FileUploadModel model = new FileUploadModel();
                    Map<String, String> fileAttributes = new HashMap<>();
                    fileAttributes.put("name", model.getUri());
                    fileAttributes.put("mimeType", tika.detect(folderFile));
                    fileAttributes.put("size", folderFileSize);
                    finalPaths.put(file, fileAttributes);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return finalPaths;
    }

    private boolean writeToFile(InputStream inputStream, String fileLocation) {
        try {
            System.out.println("\n\n FileLocation: " + fileLocation + " *****\n\n");
            File f = new File(fileLocation);
            boolean dirCreated = false;
            if (!f.getParentFile().exists()) {
                dirCreated = f.getParentFile().mkdirs();
            }
            if (!dirCreated) logger.error("Directory creation failed");
            OutputStream out;
            int read;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(fileLocation);
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unused")
    public List<MyUpload> handleBulkUpload(HttpServletRequest request, MODULE module, List<FormDataBodyPart> files) {
        List<MyUpload> savedFiles = new ArrayList<>();
        try {
            CommonProfile profile = AuthUtil.getProfileFromRequest(request);
            Long userId = Long.parseLong(profile.getId());
            Tika tika = new Tika();
            String hash = String.join("", "ibpmu-", UUID.randomUUID().toString());
            String myUploadsPath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads + File.separatorChar + userId;
            String tempPath = storageBasePath + File.separatorChar + BASE_FOLDERS.temp + File.separatorChar + userId;
            for (FormDataBodyPart file : files) {
                String fileName = file.getContentDisposition().getFileName();
                String contentType = tika.detect(fileName);
                File f;
                if (contentType.endsWith("zip")) {
                    String zipPath = tempPath + File.separatorChar + hash + File.separatorChar + fileName;
                    boolean isZipCreated = writeToFile(file.getEntityAs(InputStream.class), zipPath);
                    f = new File(zipPath);
                    if (isZipCreated) {
                        List<MyUpload> extractedFiles = AppUtil.parseZipFiles(myUploadsPath, f.getCanonicalPath(), module);
                        savedFiles.addAll(extractedFiles);
                    }

                } else {
                    f = new File(myUploadsPath + File.separatorChar + hash + File.separatorChar + file.getFormDataContentDisposition().getFileName());
                    savedFiles.add(saveFile(file.getEntityAs(InputStream.class), module, file.getContentDisposition(), hash, userId));
                }
                boolean deleted = f.delete() && f.getParentFile().delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return savedFiles;
    }

    public Map<String, Object> moveFilesFromUploads(Long userId, List<String> fileList, BASE_FOLDERS folder, MODULE module) {
        Map<String, Object> finalPaths = new HashMap<>();
        try {
            String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.getFolder()
                    + File.separatorChar + userId;
            Map<String, String> files = new HashMap<>();
            Tika tika = new Tika();

            // stream the user directory and prepare a map of file and file path
            java.nio.file.Files
                    .find(java.nio.file.Paths.get(basePath),
                            Integer.MAX_VALUE,
                            (f, bfa) -> {
                                String type = tika.detect(f.getFileName().toString());
                                return java.nio.file.Files.isRegularFile(f) && AppUtil.filterFileTypeForModule(type, module);
                            })
                    .forEach(file -> {
                        File f = file.toFile();
                        try {
                            files.put(f.getName(), f.getCanonicalPath().substring(basePath.length()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            System.out.println("\n\n***** All files in User " + userId + ": " + files + " *****\n\n");

            List<String> filesWithPath = new ArrayList<>();
            for (String file : fileList) {
                if (files.containsKey(file)) {
                    filesWithPath.add(files.get(file));
                }
            }
            Map<String, Object> result = moveFilesFromUploads(userId, filesWithPath, folder.toString());
            if (result != null && !result.isEmpty()) {
                for (Map.Entry<String, Object> file : result.entrySet()) {
                    String fileNameWithPath = file.getKey();
                    finalPaths.put(fileNameWithPath.substring(fileNameWithPath.lastIndexOf(File.separatorChar) + 1), file.getValue());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return finalPaths;
    }
}
