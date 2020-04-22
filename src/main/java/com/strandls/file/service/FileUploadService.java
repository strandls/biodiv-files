package com.strandls.file.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.tika.Tika;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.strandls.file.model.FileMetaData;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.model.MyUpload;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.ImageUtil.BASE_FOLDERS;
import com.strandls.file.util.ThumbnailUtil;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;

public class FileUploadService {

	@Inject
	private FileMetaDataService fileMetaDataService;

	String storageBasePath = null;

	public FileUploadService() {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}

		storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv-image");
	}

	public List<FileUploadModel> uploadMultipleFiles(String directory, FormDataBodyPart body,
			HttpServletRequest request, String hashKey) throws IOException {

		List<FileUploadModel> mutipleFiles = new ArrayList<FileUploadModel>();
		for (BodyPart part : body.getParent().getBodyParts()) {
			InputStream is = part.getEntityAs(InputStream.class);
			ContentDisposition contentDisposition = part.getContentDisposition();
			FileUploadModel file = uploadFile(directory, is, (FormDataContentDisposition) contentDisposition, request,
					hashKey);
			if (hashKey == null || "".equals(hashKey))
				hashKey = file.getHashKey();
			mutipleFiles.add(file);
		}
		return mutipleFiles;
	}

	private FileUploadModel uploadFile(String directory, InputStream inputStream, FormDataContentDisposition fileDetails,
			HttpServletRequest request, String hashKey) throws IOException {

		FileUploadModel fileUploadModel = new FileUploadModel();

		String fileName = fileDetails.getFileName();

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
		boolean isFileCreated = writeToFile(is, file.getAbsolutePath());
		MyUpload uploadModel = new MyUpload();
		if (isFileCreated) {
			String probeContentType = tika.detect(fileName);
			uploadModel.setFileName(file.getName());
			uploadModel.setHashKey(hash);
			uploadModel
					.setPath(File.separatorChar + file.getParentFile().getName() + File.separatorChar + file.getName());
			uploadModel.setType(probeContentType);
			String exifData = AppUtil.getExifGeoData(file.getAbsolutePath());
			if (exifData != null && !exifData.isEmpty() && exifData.contains("*")) {
				String[] coordinates = exifData.split("[*]");
				if (coordinates.length == 2) {
					uploadModel.setLatitude(AppUtil.calculateValues(coordinates[0]));
					uploadModel.setLongitude(AppUtil.calculateValues(coordinates[1]));
				}
			}
		} else {
			throw new Exception("File not created");
		}
		return uploadModel;
	}

	public List<MyUpload> getFilesFromUploads(Long userId) {
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
						String exifData = AppUtil.getExifGeoData(tmpFile.getAbsolutePath());
						if (exifData != null && !exifData.isEmpty() && exifData.contains("*")) {
							String[] coordinates = exifData.split("[*]");
							if (coordinates.length == 2) {
								uploadModel.setLatitude(AppUtil.calculateValues(coordinates[0]));
								uploadModel.setLongitude(AppUtil.calculateValues(coordinates[1]));
							}
						}
						uploadModel.setPath(File.separatorChar + tmpFile.getParentFile().getName() + File.separatorChar
								+ tmpFile.getName());
						uploadModel.setType(probeContentType);
						return uploadModel;
					}).collect(Collectors.toList());

			files.addAll(filesList);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return files;
	}
	
	public boolean deleteFilesFromMyUploads(Long userId, String fileName) {
		boolean isDeleted = false;
		String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString() + File.separatorChar +
				userId;
		File f = new File(basePath + File.separatorChar + fileName);
		if (f.exists()) {
			isDeleted = f.delete() && f.getParentFile().delete();
		}
		return isDeleted;
	}

	public Map<String, String> moveFilesFromUploads(Long userId, List<String> fileList) throws Exception {
		Map<String, String> finalPaths = new HashMap<>();
		try {
			String basePath = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString() + File.separatorChar
					+ userId;
			String hash = UUID.randomUUID().toString();

			System.out.println("\n\n***** Hash: " + hash + "*****\n\n");
			String existingHash = fileList.stream().filter(path -> !path.startsWith(File.separatorChar + "ibpmu-")).findAny()
					.orElse(null);
			if (existingHash != null && !existingHash.isEmpty()) {
				existingHash = existingHash.substring(1);
				existingHash = existingHash.substring(0, existingHash.indexOf(File.separatorChar));				
			}

			System.out.println("\n\n***** Existing Hash: " + existingHash + "*****\n\n");
			for (String file : fileList) {
				File f = new File(basePath + file);
				if (file.startsWith(File.separatorChar + "ibpmu-")) {
					if (f.exists()) {
						String fileName = f.getName();
						FileUploadModel model = uploadFile(f.getAbsolutePath(), BASE_FOLDERS.observations.toString(),
								existingHash == null ? hash : existingHash, fileName);
						finalPaths.put(file, model.getUri());


						System.out.println("\n\n***** New Path: " + model.getUri() + "*****\n\n");
						f.getParentFile().delete();
					}
				} else {
					System.out.println("\n\n***** Existing Path: " + file + "*****\n\n");
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
