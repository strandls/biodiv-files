package com.strandls.file.util;

import java.io.File;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

public class ImageUtil {
	
	public static enum BASE_FOLDERS {
		observations("observations"),
		img("img"),
		species("species"), 
		userGroups("userGroups"), 
		users("users"), 
		traits("traits"),
		myUploads("myUploads"),
		thumbnails("thumbnails"),
		documents(String.join(String.valueOf(File.separatorChar), "content", "documents")),
		temp("temp"),
		datasets(String.join(String.valueOf(File.separatorChar), "content", "datasets"));
		
		private String folder;
		
		private BASE_FOLDERS(String folder) {
			this.folder = folder;
		}
		
		public String getFolder() {
			return folder;
		}
	};
	
	public static boolean checkFolderExistence(String directory) {
		boolean hasFolder = false;
		if (directory == null) {
			return hasFolder;
		}
		for (BASE_FOLDERS folders: BASE_FOLDERS.values()) {
			if (folders.name().equalsIgnoreCase(directory)) {
				hasFolder = true;
				break;
			}
		}
		return hasFolder;
	}
	
	public static BASE_FOLDERS getFolder(String directory) {
		if (directory == null || directory.isEmpty()) {
			return null;
		}
		for (BASE_FOLDERS folders: BASE_FOLDERS.values()) {
			if (folders.name().equals(directory)) {
				return folders;
			}
		}
		return null;
	}
	
	public static void toWEBP(HttpServletRequest req, File src, File dest) throws Exception {
		String realPath = req.getServletContext().getRealPath("/WEB-INF/");
		String command = "/bin/sh " + realPath + "cwebp -lossless " + src.getPath() + " -o " + dest.getPath();
		Runtime.getRuntime().exec(command).waitFor();
	}
}
