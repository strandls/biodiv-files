package com.strandls.file.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.CacheControl;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.file.model.MyUpload;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class AppUtil {

	private static final List<String> PREVENTIVE_TOKENS = Arrays.asList("&", "|", "`", "$", ";");
	private static final int QUALITY = 90;

	private static final Logger logger = LoggerFactory.getLogger(AppUtil.class);

	public static final Map<MODULE, List<String>> ALLOWED_CONTENT_TYPES = new HashMap<>();

	static {
		ALLOWED_CONTENT_TYPES.put(MODULE.OBSERVATION, Arrays.asList("image", "video", "audio"));
		ALLOWED_CONTENT_TYPES.put(MODULE.DOCUMENT, Arrays.asList("pdf"));
		ALLOWED_CONTENT_TYPES.put(MODULE.SPECIES, Arrays.asList());
		ALLOWED_CONTENT_TYPES.put(MODULE.DATASETS, Arrays.asList("vnd.ms-excel", "spreadsheetml.sheet", "csv"));
	};

	public enum MODULE {
		OBSERVATION, SPECIES, DOCUMENT, DATASETS
	}

	public enum FILE_UPLOAD_TYPES {
		UPLOAD, MOVE
	}

	public static enum BASE_FOLDERS {
		observations("observations"), img("img"), species("species"), userGroups("userGroups"), users("users"),
		pages("pages"), traits("traits"), myUploads("myUploads"), thumbnails("thumbnails"), landscape("landscape"),
		documents(String.join(String.valueOf(File.separatorChar), "content", "documents")), temp("temp"),
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
		for (BASE_FOLDERS folders : BASE_FOLDERS.values()) {
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
		for (BASE_FOLDERS folders : BASE_FOLDERS.values()) {
			if (folders.name().equalsIgnoreCase(directory)) {
				return folders;
			}
		}
		return null;
	}

	public static MODULE getModule(String moduleName) {
		if (moduleName == null || moduleName.isEmpty()) {
			return null;
		}
		for (MODULE module : MODULE.values()) {
			if (module.name().equalsIgnoreCase(moduleName.toLowerCase())) {
				return module;
			}
		}
		return null;
	}

	public static List<MyUpload> parseZipFiles(String storageBasePath, String filePath, MODULE module) {
		List<MyUpload> files = new ArrayList<>();
		try {
			ZipFile zipFile = new ZipFile(filePath);
			List<FileHeader> headers = zipFile.getFileHeaders();
			Iterator<FileHeader> it = headers.iterator();
			Tika tika = new Tika();
			String hash = String.join("", "ibpmu-", UUID.randomUUID().toString());
			System.out.println("==================Bulk Upload for Unzip started==================");
			while (it.hasNext()) {
				String destinationPath = storageBasePath + File.separatorChar + hash + File.separatorChar;
				FileHeader header = it.next();
				final String contentType = tika.detect(header.getFileName());
				boolean allowedType = ALLOWED_CONTENT_TYPES.get(module).stream()
						.anyMatch((type) -> contentType.toLowerCase().startsWith(type)
								|| contentType.toLowerCase().endsWith(type));
				if (!allowedType) {
					continue;
				}
				zipFile.extractFile(header, destinationPath);
				MyUpload upload = new MyUpload();
				upload.setType(contentType);
				String finalPath = String.join("", destinationPath.substring(0, destinationPath.length() - 1),
						header.getFileName());
				upload.setPath(finalPath.substring(storageBasePath.length()));
				upload.setFileName(header.getFileName());
				upload.setHashKey(hash);
				files.add(upload);
			}
			System.out.println("=====================Completed UnZip bulk Uploads=================");
		} catch (ZipException ex) {
			logger.error(ex.getMessage());
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
		return files;
	}

	public static boolean filterFileTypeForModule(String contentType, MODULE module) {
		boolean addToList = false;
		if (contentType == null) {
			return addToList;
		}
		addToList = ALLOWED_CONTENT_TYPES.get(module).stream().anyMatch(type -> {
			return contentType.toLowerCase().startsWith(type) || contentType.toLowerCase().endsWith(type);
		});
		return addToList;
	}

	public static boolean filterFileByName(String filename, List<String> fileList) {
		if (fileList.size() > 1) {
			return true;
		}
		return filename.contains(fileList.get(0));
	}

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
				if (!f.isDirectory() && name.contains(".")
						&& name.substring(0, name.indexOf(".")).equalsIgnoreCase(fileName)) {
					expectedFile = f;
					break;
				}
			}
		}
		return expectedFile;
	}

	public static String generateCommand(String filePath, Integer w, Integer h, String format, Integer quality,
			String fit) {
		List<String> commands = new ArrayList<>();
		StringBuilder command = new StringBuilder();
		String fileName = filePath.substring(0, filePath.lastIndexOf("."));
		command.append("convert").append(" ").append(filePath).append(" ").append("-auto-orient").append(" ")
				.append("-resize").append(" ");
		if (h != null && w != null && fit.equalsIgnoreCase("center")) {
			command.append(w).append("x").append(h).append("^");
			command.append(" ").append("-gravity").append(" ").append("center").append(" ").append("-extent")
					.append(" ");
			command.append(w).append("x").append(h).append(" ");
		} else if (h != null && w != null) {
			command.append(w).append("x").append(h).append("!");
		} else if (h != null) {
			command.append("x").append(h);
		} else if (w != null) {
			command.append(w);
		}
		command.append(" ");
		command.append("-quality").append(" ").append(quality == null ? QUALITY : quality);
		command.append(" ");
		if (fileName.contains(" ")) {
			command.append("'").append(fileName + "-mod").append("_").append(w).append("x").append(h).append(".")
					.append(format).append("'");
		} else {
			command.append(fileName + "-mod").append("_").append(w).append("x").append(h).append(".").append(format);
		}
		commands.add(command.toString());
		return String.join(" ", commands).trim();
	}

	public static String generateCommand(String filePath, String outputFilePath, Integer w, Integer h, String format,
			Integer quality, String fit) {
		List<String> commands = new ArrayList<>();
		StringBuilder command = new StringBuilder();
		String fileName = filePath.substring(0, filePath.lastIndexOf("."));
		String fileNameWithoutPrefix = fileName.substring(fileName.lastIndexOf(File.separatorChar));
		String finalFilePath = outputFilePath + fileNameWithoutPrefix + "-mod";
		command.append("convert").append(" ");
		if (filePath.contains(" ")) {
			command.append("'").append(filePath).append("'");
		} else {
			command.append(filePath);
		}
		command.append(" ").append("-auto-orient").append(" ").append("-resize").append(" ");
		if (h != null && w != null && fit.equalsIgnoreCase("center")) {
			command.append(w).append("x").append(h).append("^");
			command.append(" ").append("-gravity").append(" ").append("center").append(" ").append("-extent")
					.append(" ");
			command.append(w).append("x").append(h).append(" ");
		} else if (h != null && w != null) {
			command.append(w).append("x").append(h).append("!");
		} else if (h != null) {
			command.append("x").append(h);
		} else if (w != null) {
			command.append(w);
		}
		command.append(" ");
		command.append("-quality").append(" ").append(quality == null ? QUALITY : quality);
		command.append(" ");
		if (finalFilePath.contains(" ")) {
			command.append("'").append(finalFilePath).append("_").append(w).append("x").append(h).append("_")
					.append(fit).append(".").append(format).append("'");
		} else {
			command.append(finalFilePath).append("_").append(w).append("x").append(h).append("_").append(fit)
					.append(".").append(format);
		}
		commands.add(command.toString());
		return String.join(" ", commands).trim();
	}

	public static String generateCommandLogo(String filePath, String outputFilePath, Integer w, Integer h,
			String format) {
		List<String> commands = new ArrayList<>();
		StringBuilder command = new StringBuilder();
		String fileName = filePath.substring(0, filePath.lastIndexOf("."));
		String fileNameWithoutPrefix = fileName.substring(fileName.lastIndexOf(File.separatorChar));
		String finalFilePath = outputFilePath + fileNameWithoutPrefix + "-mod";
		command.append("convert").append(" ");
		if (filePath.contains(" ")) {
			command.append("'").append(filePath).append("'");
		} else {
			command.append(filePath);
		}
		command.append(" ").append("-auto-orient").append(" ").append("-resize").append(" ");
		if (h != null && w != null) {
			command.append(w).append("x").append(h);
			command.append(" ").append("-gravity").append(" ").append("center").append(" ").append("-extent")
					.append(" ");
			command.append(w).append("x").append(h).append(" ");
		}
		command.append(" ");
		command.append("-quality").append(" ").append(QUALITY);
		command.append(" ");
		if (finalFilePath.contains(" ")) {
			command.append("'").append(finalFilePath).append("_").append(w).append("x").append(h).append(".")
					.append(format).append("'");
		} else {
			command.append(finalFilePath).append("_").append(w).append("x").append(h).append(".").append(format);
		}
		commands.add(command.toString());
		return String.join(" ", commands).trim();
	}

	public static File getResizedImage(String command) {
		File resizedImage = null;
		try {
			command = command.replaceAll("'", "");
			String delimiter = "-quality " + String.valueOf(QUALITY);
			resizedImage = new File(command.substring(command.indexOf(delimiter) + delimiter.length()).trim());
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
			if (!PREVENTIVE_TOKENS.stream().filter(symbol -> command.contains(symbol)).findAny().isPresent()) {
				String[] commands = { "/bin/sh", "-c", command };
				p = Runtime.getRuntime().exec(commands);
				br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = br.readLine()) != null) {
					output.append(line);
				}
				p.waitFor();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return output.toString();
	}

	public static boolean executeCommandWithExitValue(String command) {
		Process p = null;
		boolean output = false;
		try {
			if (!PREVENTIVE_TOKENS.stream().filter(symbol -> command.contains(symbol)).findAny().isPresent()) {
				String[] commands = { "/bin/sh", "-c", command };
				p = Runtime.getRuntime().exec(commands);
				output = p.waitFor(5, TimeUnit.SECONDS);
			}
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
		if (expression.isEmpty() || !expression.contains(",")) {
			return null;
		}
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

	public static String getExifData(String fileName) {
		String command = "identify -format \"%[EXIF:GPSLatitude]*%[EXIF:GPSLongitude]*%[EXIF:DateTime]\" '" + fileName
				+ "'";
		return executeCommand(command);
	}

}
