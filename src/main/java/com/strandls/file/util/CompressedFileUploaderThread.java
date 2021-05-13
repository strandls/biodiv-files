package com.strandls.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.tika.Tika;

import com.google.common.io.Files;
import com.strandls.file.service.FileUploadService;
import com.strandls.file.util.AppUtil.MODULE;

public class CompressedFileUploaderThread implements Runnable {

	
	private Long userId;
	private File file;
	private String absDestinationPath;
	private String sourceDir;
	private MODULE module;
	private FileUploadService fileUploadService;

	/**
	 * 
	 */
	public CompressedFileUploaderThread() {
		super();
	}

	/**
	 * 
	 * @param userId
	 * @param file
	 * @param absDestinationPath
	 * @param sourceDir
	 * @param module
	 */
	public CompressedFileUploaderThread(Long userId, File file, String absDestinationPath, String sourceDir,
			MODULE module) {
		super();
		this.userId = userId;
		this.file = file;
		this.absDestinationPath = absDestinationPath;
		this.sourceDir = sourceDir;
		this.module = module;
		this.fileUploadService = new FileUploadService();
	}

	@Override
	public void run() {

		try {
			extractFilesToMyUploads(userId, file, absDestinationPath, sourceDir, module);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void extractFilesToMyUploads(Long userId, File file, String absDestinationPath, String sourceDir,
			MODULE module) throws Exception {
		Tika tika = new Tika();
		String probeContentType = tika.detect(file);
		if (probeContentType.contains("7z")) {
			List<File> extractedFiles = extract7ZipFile(file, sourceDir);
			for (File extFile : extractedFiles) {
				boolean allowedContentType = AppUtil.filterFileTypeForModule(tika.detect(extFile), module);
				if (!allowedContentType) {
					extractedFiles.forEach((item) -> {
						item.delete();
					});
					throw new Exception("Invalid file type. Allowed types are "
							+ String.join(", ", AppUtil.ALLOWED_CONTENT_TYPES.get(module)));
				}
				InputStream targetStream = new FileInputStream(extFile);
				String hash = UUID.randomUUID().toString();

				fileUploadService.saveFile(targetStream, module, extFile.getName(), hash, userId);
				extFile.delete();
			}

		} else if (probeContentType.endsWith("zip")) {
			AppUtil.parseZipFiles(absDestinationPath, file.getCanonicalPath(), module);
		}
		System.out.println("***Completed compressed file extraction to:  "+absDestinationPath+"***");
	}

	public List<File> extract7ZipFile(File file, String basePath) {

		List<File> extractFileList = new ArrayList<File>();
		try (SevenZFile sevenZFile = new SevenZFile(file)) {
			SevenZArchiveEntry entry;
			while ((entry = sevenZFile.getNextEntry()) != null) {
				File outFile = new File(basePath + entry.getName());
				byte[] content = new byte[(int) entry.getSize()];
				sevenZFile.read(content);
				Files.write(content, outFile);
				extractFileList.add(outFile);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return extractFileList;
	}

}
