package com.strandls.file.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		boolean isFileGenerated = false;
		try {
			isFileGenerated = executeCommandWithExitValue(command);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return isFileGenerated;
	}

	public static File findFile(String filePath) throws IOException {
		File expectedFile = null;
		String dir = filePath.substring(0, filePath.lastIndexOf(File.separatorChar));
		String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.lastIndexOf("."));
		File file = new File(dir);
		if (file.exists()) {
			for (File f : file.listFiles()) {
				String name = f.getCanonicalFile().getName();
				if (!f.isDirectory() && name.substring(0, name.indexOf(".")).equalsIgnoreCase(fileName)) {
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

	public static String generateCommand(String filePath, String outputFilePath, Integer w, Integer h, String format,
			Integer quality) {
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
		command.append(outputFilePath + fileNameWithoutPrefix).append("_").append(w).append("x").append(h).append(".")
				.append(format);
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
	
	public static String executeCommand(String command) {
		Process p = null;
		StringBuilder output = new StringBuilder();
		BufferedReader br = null;
		try {
			String[] commands = { "/bin/sh", "-c", command };
			p = Runtime.getRuntime().exec(commands);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				output.append(line);
			}
			p.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return output.toString();
	}
	
	public static boolean executeCommandWithExitValue(String command) {
		Process p = null;
		boolean output = false;
		try {
			String[] commands = { "/bin/sh", "-c", command };
			p = Runtime.getRuntime().exec(commands);
			output = p.waitFor(5, TimeUnit.SECONDS);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return output;
	}

	public static Double evaluateExpression(String expression) {
		if (expression.contains("/")) {
			String[] values = expression.split("/");
			double v1 = Double.parseDouble(values[0]);
			double v2 = Double.parseDouble(values[1]);
			return v1 / v2;
		}
		return null;
	}
	
	public static Double calculateValues(String expression) {
		Double value = null;
		try {
			String[] values = expression.split(",");
			Double h = evaluateExpression(values[0]);
			Double m = evaluateExpression(values[1]);
			Double s = evaluateExpression(values[2]);
			if (h != null && m != null && s != null) {
				value = new Double(h + (m / 60) + (s / 3600));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return value;
	}
	
	public static String getExifGeoData(String fileName) {
		String command = "identify -format \"%[EXIF:GPSLatitude]*%[EXIF:GPSLongitude]\" " + fileName;
		return executeCommand(command);
	}

}
