package com.strandls.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.util.UUID;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.file.service.FileUploadService;
import com.strandls.file.util.AppUtil.MODULE;

public class CompressedFileUploaderThread implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CompressedFileUploaderThread.class);

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
			logger.error(e.getMessage());
		}

	}

	private void extractFilesToMyUploads(Long userId, File file, String absDestinationPath, String sourceDir,
			MODULE module) throws Exception {
		Tika tika = new Tika();
		String probeContentType = tika.detect(file);
		if (probeContentType.endsWith("zip")) {
			AppUtil.parseZipFiles(absDestinationPath, file.getCanonicalPath(), module);
		}
		System.out.println("***Completed compressed file extraction to:  " + absDestinationPath + "***");
	}

}
