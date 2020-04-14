package com.strandls.file.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

	public static boolean generateFile(String command) {
		Process p = null;
		boolean isFileGenerated = false;
		try {
			String[] commands = { "/bin/sh", "-c", command };
			p = Runtime.getRuntime().exec(commands);
			isFileGenerated = p.waitFor(5, TimeUnit.SECONDS);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return isFileGenerated;
	}

	public static File findFile(String filePath) throws IOException {
		File expectedFile = null;
		String dir = filePath.substring(0, filePath.lastIndexOf(File.separatorChar));
		String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.lastIndexOf("."));
		String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		File file = new File(dir);
		if (file.exists()) {
			for (File f : file.listFiles()) {
				String name = f.getCanonicalFile().getName();
				String ext = name.substring(name.indexOf(".") + 1);
				if (!f.isDirectory() && name.substring(0, name.indexOf(".")).equals(fileName)
						&& ext.toLowerCase().equalsIgnoreCase(fileExtension.toLowerCase())) {
					expectedFile = f;
					break;
				}
			}
		}
		return expectedFile;
	}

	public static String generateCommand(String filePath, Integer w, Integer h, String format, Integer quality) {
		List<String> commands = new ArrayList<>();
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
			command.append("-quality").append(" ").append(quality == null ? 90 : quality);
		}
		command.append(" ");
		command.append(fileName).append("_").append(w).append("x").append(h).append(".").append(format);
		commands.add(command.toString());
		return String.join(" ", commands);
	}
	
	public static String generateCommand(String filePath, String outputFilePath, Integer w, Integer h, String format, Integer quality) {
		List<String> commands = new ArrayList<>();
		StringBuilder command = new StringBuilder();
		String fileName = filePath.substring(0, filePath.lastIndexOf("."));
		String fileNameWithoutPrefix = fileName.substring(fileName.lastIndexOf(File.separatorChar));
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
			command.append("-quality").append(" ").append(quality == null ? 90 : quality);
		}
		command.append(" ");
		command.append(outputFilePath + fileNameWithoutPrefix).append("_").append(w).append("x").append(h).append(".").append(format);
		commands.add(command.toString());
		return String.join(" ", commands);
	}

	public static File getResizedImage(String command) {
		File resizedImage = null;
		try {
			String[] commands = command.split(" ");
			resizedImage = new File(commands[commands.length - 1]);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resizedImage;
	}

}
