package com.strandls.file;

import java.io.File;

public class ApiContants {
	
	public static final String UPLOAD    = "upload";
	public static final String GET  = "get";
	
	public final static String ROOT_PATH = System.getProperty("user.home") + File.separatorChar +
			"apps" + File.separatorChar + "biodiv-image";
	
	public static final String ORIGINAL = "original";
	public static final String DOWNLOAD = "download";
	public static final String DWCFILE = "/dwcfile";
	public static final String MY_UPLOADS = "/my-uploads";
	public static final String MOVE_FILES = "/move-files";
	public static final String REMOVE_FILE = "/remove-file";
}
