package com.strandls.file.util;

import java.io.File;
import java.nio.file.Paths;
	
import javax.servlet.http.HttpServletRequest;

public class ImageUtil {
	public static void toWEBP(HttpServletRequest req, File src, File dest) throws Exception {
		String realPath = req.getServletContext().getRealPath("/WEB-INF/");
		String command = "/bin/sh " + realPath + "cwebp " + src.getPath() + " -o " + dest.getPath();
		Runtime.getRuntime().exec(command).waitFor();
	}
}
