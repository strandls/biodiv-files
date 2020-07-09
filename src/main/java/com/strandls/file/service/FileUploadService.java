package com.strandls.file.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import com.google.common.io.Files;
import com.strandls.file.model.FileMetaData;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.model.MyUpload;
import com.strandls.file.model.comparator.UploadDateSort;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.ImageUtil.BASE_FOLDERS;
import com.strandls.file.util.ThumbnailUtil;

public class FileUploadService {

	@Inject
	private FileMetaDataService fileMetaDataService;

	String storageBasePath = null;
	SimpleDateFormat sdf;

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

	public FileUploadModel uploadFile(String directory, InputStream inputStream, FormDataContentDisposition fileDetails,
			HttpServletRequest request, String nestedFolder, String hashKey, boolean resourceFolder)
			throws IOException {

		FileUploadModel fileUploadModel = new FileUploadModel();

		String fileName = fileDetails.getFileName();

		String fileExtension = Files.getFileExtension(fileName);

		String folderName = "";
		if (nestedFolder != null && nestedFolder.length() > 0) {
			folderName += String.join(String.valueOf(File.separatorChar), nestedFolder.split(",")) + File.separatorChar;
		} else {
			throw new IOException("Invalid NestedFolder Name");
		}
		folderName = "".equals(hashKey) ? UUID.randomUUID().toString() : hashKey;
		if (resourceFolder) {
			folderName += File.separatorChar + "resources";
		}
		String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar + folderName;
		Tika tika = new Tika();
		String probeContentType = tika.detect(fileName);

		if (probeContentType == null || !probeContentType.startsWith("image")) {
			fileUploadModel.setError("Invalid file type. Only image type allowed.");
			return fileUploadModel;
		} else {
			fileUploadModel.setType(probeContentType);
		}

		FileMetaData fileMetaData = new FileMetaData();
		fileMetaData.setFileName(fileName);
		fileMetaData.setPath(folderName);
		fileMetaDataService.save(fileMetaData);

		String generatedFileName = fileMetaData.getId() + "." + fileExtension;

		String filePath = dirPath + File.separatorChar + generatedFileName;
		System.out.println("\n\n FileLocation: " + filePath + " *****\n\n");
		File file = new File(filePath);
		if (!file.getCanonicalPath().startsWith(storageBasePath)) {
			throw new IOException("Invalid folder");
		}
		boolean uploaded = writeToFile(inputStream, filePath);

		fileUploadModel.setUploaded(uploaded);

		if (probeContentType.startsWith("image")) {
			Thread thread = new Thread(new ThumbnailUtil(filePath, dirPath, fileMetaData.getId(), fileExtension));
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

		FileMetaData fileMetaData = new FileMetaData();
		fileMetaData.setFileName(fileName);
		fileMetaData.setPath(folderName);
		fileMetaDataService.save(fileMetaData);

		String generatedFileName = fileMetaData.getId() + "." + fileExtension;

		String filePath = dirPath + File.separatorChar + generatedFileName;
		File destFile = new File(filePath);
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		Path path = java.nio.file.Files.move(Paths.get(source), Paths.get(filePath), StandardCopyOption.ATOMIC_MOVE);
		boolean uploaded = path != null;

		fileUploadModel.setUploaded(uploaded);

		if (probeContentType.startsWith("image")) {
			Thread thread = new Thread(new ThumbnailUtil(filePath, dirPath, fileMetaData.getId(), fileExtension));
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

	public MyUpload saveFile(InputStream is, FormDataContentDisposition fileDetails, String hash, Long userId)
			throws Exception {
		String dir = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString() + File.separatorChar
				+ userId + File.separatorChar + hash;
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}
		Tika tika = new Tika();
		String fileName = dir + File.separatorChar + fileDetails.getFileName();
		File file = new File(fileName);
		if (file.getCanonicalPath().startsWith(dir) && file.getCanonicalFile().exists()) {
			return getExistingFileData(file);
		}
		String probeContentType = tika.detect(fileName);
		if (probeContentType == null || !probeContentType.startsWith("image") && !probeContentType.startsWith("audio")
				&& !probeContentType.startsWith("video")) {
			throw new Exception("Invalid file type. Allowed types are image, audio and video.");
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
			String exifData = AppUtil.getExifData(file.getAbsolutePath());
			String[] data = exifData.split("\\*");
			if (exifData != null && !exifData.isEmpty() && exifData.contains("*")) {
				int dataLength = data.length;
				if (dataLength == 1) {
					String dateStr = data[0];
					Date capturedDate = null;
					try {
						if (!dateStr.isEmpty()) {
							capturedDate = sdf.parse(dateStr);
							attributes = java.nio.file.Files.readAttributes(Paths.get(file.toURI()),
									BasicFileAttributes.class);
							Date uploadedDate = new Date(attributes.creationTime().toMillis());
							uploadModel.setDateUploaded(uploadedDate);
						}
						uploadModel.setDateCreated(capturedDate);
					} catch (Exception ex) {
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
					}
				}
				attributes = java.nio.file.Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class);
				Date uploadedDate = new Date(attributes.creationTime().toMillis());
				uploadModel.setDateUploaded(uploadedDate);
			}
		} else {
			throw new Exception("File not created");
		}
		return uploadModel;
	}

	public List<MyUpload> getFilesFromUploads(Long userId) throws Exception {
		List<MyUpload> files = new ArrayList<>();
		String userDir = BASE_FOLDERS.myUploads.toString() + File.separatorChar + userId;
		try {
			Tika tika = new Tika();
			List<MyUpload> filesList = java.nio.file.Files
					.walk(java.nio.file.Paths.get(storageBasePath + File.separatorChar + userDir))
					.filter(java.nio.file.Files::isRegularFile).map(f -> {
						File tmpFile = f.toFile();
						String probeContentType = tika.detect(tmpFile.getName());
						MyUpload uploadModel = new MyUpload();
						uploadModel.setHashKey(tmpFile.getParentFile().getName());
						uploadModel.setFileName(tmpFile.getName());
						String exifData = AppUtil.getExifData(tmpFile.getAbsolutePath());
						if (exifData != null && !exifData.isEmpty() && exifData.contains("*")) {
							String[] data = exifData.split("\\*");
							int dataLength = data.length;
							BasicFileAttributes attributes = null;
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
								}
							}
							Date uploadedDate = null;
							try {
								attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()),
										BasicFileAttributes.class);
								uploadedDate = new Date(attributes.creationTime().toMillis());
								uploadModel.setDateUploaded(uploadedDate);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						uploadModel.setPath(File.separatorChar + tmpFile.getParentFile().getName() + File.separatorChar
								+ tmpFile.getName());
						uploadModel.setType(probeContentType);
						return uploadModel;
					}).collect(Collectors.toList());
			files.addAll(filesList);
			Collections.sort(files, new UploadDateSort());
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
		String exifData = AppUtil.getExifData(tmpFile.getAbsolutePath());
		if (exifData != null && !exifData.isEmpty() && exifData.contains("*")) {
			System.out.println("\n\n***** Exif: " + exifData + " *****\n\n");
			String[] data = exifData.split("\\*");
			int dataLength = data.length;
			BasicFileAttributes attributes;
			if (dataLength == 1) {
				String dateStr = data[0];
				Date capturedDate = null;
				try {
					if (!dateStr.isEmpty()) {
						capturedDate = sdf.parse(dateStr);
					}
					uploadModel.setDateCreated(capturedDate);
				} catch (Exception ex) {
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
				}
			}
			attributes = java.nio.file.Files.readAttributes(Paths.get(tmpFile.toURI()), BasicFileAttributes.class);
			Date uploadedDate = new Date(attributes.creationTime().toMillis());
			uploadModel.setDateUploaded(uploadedDate);
		}
		uploadModel.setPath(
				File.separatorChar + tmpFile.getParentFile().getName() + File.separatorChar + tmpFile.getName());
		uploadModel.setType(probeContentType);
		return uploadModel;
	}

	public boolean deleteFilesFromMyUploads(Long userId, String fileName) throws IOException {
		boolean isDeleted = false;
		String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString() + File.separatorChar
				+ userId;
		File f = new File(basePath + File.separatorChar + fileName);
		if (f.exists() && java.nio.file.Files.isRegularFile(Paths.get(f.toURI()))
				&& f.getCanonicalPath().startsWith(basePath)) {
			isDeleted = f.delete() && f.getParentFile().delete();
		}
		return isDeleted;
	}

	public Map<String, String> moveFilesFromUploads(Long userId, List<String> fileList) throws Exception {
		Map<String, String> finalPaths = new HashMap<>();
		try {
			String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString()
					+ File.separatorChar + userId;
			String hash = UUID.randomUUID().toString();
			String existingHash = fileList.stream().filter(path -> !path.startsWith(File.separatorChar + "ibpmu-"))
					.findAny().orElse(null);
			if (existingHash != null && !existingHash.isEmpty()) {
				existingHash = existingHash.substring(1);
				existingHash = existingHash.substring(0, existingHash.indexOf(File.separatorChar));
			}

			for (String file : fileList) {
				File f = new File(basePath + file);
				if (file.startsWith(File.separatorChar + "ibpmu-")) {
					if (f.exists()) {
						String fileName = f.getName();
						FileUploadModel model = uploadFile(f.getAbsolutePath(), BASE_FOLDERS.observations.toString(),
								existingHash == null ? hash : existingHash, fileName);
						finalPaths.put(file, model.getUri());
						f.getParentFile().delete();
					}
				} else {
					finalPaths.put(file, file);
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
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
			OutputStream out = new FileOutputStream(f);
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(fileLocation));
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
}
