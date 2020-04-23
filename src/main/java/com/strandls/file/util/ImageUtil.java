package com.strandls.file.util;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

public class ImageUtil {
	
	public static enum BASE_FOLDERS {
		observations, 
		img, 
		species, 
		userGroups, 
		users, 
		traits,
		myUploads,
		thumbnails
	};
	
	public static boolean checkFolderExistence(String directory) {
		boolean hasFolder = false;
		for (BASE_FOLDERS folders: BASE_FOLDERS.values()) {
			if (folders.name().equals(directory)) {
				hasFolder = true;
				break;
			}
		}
		return hasFolder;
	}
	
	public static void toWEBP(HttpServletRequest req, File src, File dest) throws Exception {
		String realPath = req.getServletContext().getRealPath("/WEB-INF/");
		String command = "/bin/sh " + realPath + "cwebp -lossless " + src.getPath() + " -o " + dest.getPath();
		Runtime.getRuntime().exec(command).waitFor();
	}
}
