package com.strandls.file.util;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

public class ImageUtil {
	
	public static void toWEBP(HttpServletRequest req, File src, File dest) throws Exception {
		String realPath = req.getServletContext().getRealPath("/WEB-INF/");
		String command = "/bin/sh " + realPath + "cwebp -lossless " + src.getPath() + " -o " + dest.getPath();
		Runtime.getRuntime().exec(command).waitFor();
	}
}
