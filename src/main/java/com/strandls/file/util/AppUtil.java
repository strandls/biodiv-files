package com.strandls.file.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.ws.rs.core.CacheControl;

public class AppUtil {

	public static CacheControl getCacheControl() {
		CacheControl cache = new CacheControl();
		cache.setMaxAge(365 * 24 * 60 * 60);
		return cache;
	}

	public static String getDatePrefix() {
		StringBuilder prefix = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM");
		return prefix.append(sdf.format(new Date())).append("_").append(new GregorianCalendar().get(Calendar.YEAR))
				.append("_").toString();
	}
	
	public static File findFile(String filePath) throws IOException {
		File expectedFile = null;
		String dir = filePath.substring(0, filePath.lastIndexOf(File.separatorChar));
		String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.lastIndexOf("."));
		String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		File file = new File(dir);
		for (File f: file.listFiles()) {
			if (!f.isDirectory() && f.getCanonicalFile().getName().startsWith(fileName) && 
					f.getCanonicalFile().getName().endsWith(fileExtension.toLowerCase())) {
				expectedFile = f;
				break;
			}
		}
		return expectedFile;
	}

	public static String[] generateCommand(String filePath, Integer w, Integer h, String format, Integer quality) {
		List<String> commands = new ArrayList<>();
		commands.add("/bin/sh");
		commands.add("-c");
		StringBuilder command = new StringBuilder();
		String fileName = filePath.substring(0, filePath.lastIndexOf("."));
		command.append("convert").append(" ").append(filePath).append(" ").append("-auto-orient").append(" ")
			.append("-resize").append(" ");
		if (h != null && w != null) {
			command.append(w).append("x").append(h).append("!");
		} else if (h != null) {
			command.append("x").append(h);
		} else if (w != null) {
			command.append(w);
		}
		command.append(" ");
		if (format.equalsIgnoreCase("webp")) {
			command.append("-quality").append(" ").append(quality == null ? 90: quality);
		}
		command.append(" ");
		command.append(fileName).append("_").append("").append(".").append(format);
		commands.add(command.toString());
		return commands.toArray(new String[0]);
	}

}
