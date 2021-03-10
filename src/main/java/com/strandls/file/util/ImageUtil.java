package com.strandls.file.util;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

public class ImageUtil {

	public static void toWEBP(HttpServletRequest req, File src, File dest) throws Exception {
		String realPath = req.getServletContext().getRealPath("/WEB-INF/");
		String command = "/bin/sh " + realPath + "cwebp -lossless " + src.getPath() + " -o " + dest.getPath();
		Runtime.getRuntime().exec(command).waitFor();
	}
}
