package com.strandls.file.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.strandls.file.ApiContants;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.ImageUtil;
import com.strandls.file.util.AppUtil.BASE_FOLDERS;
import com.strandls.file.util.FileUtil;
import com.strandls.file.util.ThumbnailUtil;

public class FileDownloadService {

	private static final Logger logger = LoggerFactory.getLogger(FileDownloadService.class);

	String storageBasePath = null;

	public FileDownloadService() {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv-image");
	}

	public Response getFile(String hashKey, String fileName, String imageVariation) throws IOException {
		if (!ApiContants.ORIGINAL.equals(imageVariation)) {
			String extension = Files.getFileExtension(fileName);
			String fileNameWithoutExtension = Files.getNameWithoutExtension(fileName);
			fileName = fileNameWithoutExtension + "_" + imageVariation + "." + extension;
		}

		String fileLocation = storageBasePath + File.separatorChar + hashKey + File.separatorChar + fileName;
                return FileUtil.fromFileToStream(new File(fileLocation), "image/" + Files.getFileExtension(fileLocation));
	}

	public Response getCustomSizeFile(String hashKey, String fileName, int outputWidth, int outputHeight) throws IOException
			{

		String dirPath = storageBasePath + File.separatorChar + hashKey + File.separatorChar;
		String fileLocation = dirPath + fileName;

		File file = new File(fileLocation);

		BufferedImage image = ImageIO.read(file);
		int imageHeight = image.getHeight();
		int imageWidth = image.getWidth();
		double hRatio = ((double) imageHeight) / outputHeight;
		double wRatio = ((double) imageWidth) / outputWidth;

		int subImageHeight = imageHeight, subImageWidth = imageWidth;
		int x = 0, y = 0;

		if (hRatio < wRatio) {
			subImageWidth = (int) (hRatio * outputWidth);
			x = (imageWidth - subImageWidth) / 2;
		} else {
			subImageHeight = (int) (wRatio * outputHeight);
			y = (imageHeight - subImageHeight) / 2;
		}

		image = image.getSubimage(x, y, subImageWidth, subImageHeight);
		BufferedImage outputImage = ThumbnailUtil.getScaledImage(image, outputWidth, outputHeight);

		String extension = Files.getFileExtension(fileName);
		String fileNameWithoutExtension = Files.getNameWithoutExtension(fileName);
		File output = new File(
				dirPath + fileNameWithoutExtension + "_" + imageWidth + "*" + imageHeight + "." + extension);
		ImageIO.write(outputImage, extension, output);
                return FileUtil.fromFileToStream(output, "image/" + Files.getFileExtension(fileLocation));
	}

	public Response getImageResource(HttpServletRequest req, String directory, String fileName, Integer width,
			Integer height, String format) throws Exception {

		String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar;
		String fileLocation = dirPath + fileName;
		File file = AppUtil.findFile(fileLocation);
		if (file == null) {
			return Response.status(Status.NOT_FOUND).entity("File not found").build();
		}

		Tika tika = new Tika();
		String contentType = tika.detect(fileLocation);
		boolean isWebp = format.equalsIgnoreCase("webp");
		BufferedImage image = ImageIO.read(file);
		int imgHeight = image.getHeight();
		int imgWidth = image.getWidth();
		int newHeight = image.getHeight();
		int newWidth = image.getWidth();
		if (height != null) {
			if (imgHeight > height) {
				newHeight = height;
				newWidth = (height * imgWidth) / imgHeight;
			}
		}
		if (width != null) {
			if (imgWidth > width) {
				newWidth = width;
				newHeight = (width * imgHeight) / imgWidth;
			}
		}
		image = image.getSubimage(0, 0, imgWidth, imgHeight);
		String extension = Files.getFileExtension(fileName);
		BufferedImage outputImage = ThumbnailUtil.getScaledImage(image, width == null ? newWidth : width,
				height == null ? newHeight : height,
				extension.equalsIgnoreCase("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		String fileNameWithoutExtension = Files.getNameWithoutExtension(fileName);
		File output = new File(dirPath + fileNameWithoutExtension + "_" + imgWidth + "*" + imgHeight + "." + format);
		ImageIO.write(outputImage, extension, output);
		File webpOutput = null;
		if (isWebp) {
			webpOutput = new File(dirPath + fileNameWithoutExtension + "_" + imgWidth + "*" + imgHeight + "." + format);
			ImageUtil.toWEBP(req, output, webpOutput);
		}
                
                return FileUtil.fromFileToStream(isWebp ? webpOutput : output, isWebp ? "image/webp" : contentType);
	}

	public Response getImage(HttpServletRequest req, String directory, String fileName, Integer width, Integer height,
			String format, String fit, boolean preserve) {
		try {

			String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar;
			String fileLocation = dirPath + fileName;
			File file = AppUtil.findFile(fileLocation);

			if (file == null) {
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}

			String name = file.getName();

			String extension = name.substring(name.indexOf(".") + 1);
			String thumbnailFolder = storageBasePath + File.separatorChar + BASE_FOLDERS.thumbnails.getFolder()
					+ file.getParentFile().getAbsolutePath().substring(storageBasePath.length());
			String command = null;
			command = AppUtil.generateCommand(file.getAbsolutePath(), thumbnailFolder, width, height,
					preserve ? extension : format, null, fit);
			File thumbnailFile = AppUtil.getResizedImage(command);
			File resizedFile;
			Tika tika = new Tika();
			if (!thumbnailFile.exists()) {
				File folders = new File(thumbnailFolder);
				folders.mkdirs();
				boolean fileGenerated = AppUtil.generateFile(command);
				resizedFile = fileGenerated ? AppUtil.getResizedImage(command) : new File(file.toURI());
			} else {
				resizedFile = thumbnailFile;
			}
			logger.info("[files-api] Resized File: {}.", resizedFile.getName());
			String detactedContentType = tika.detect(resizedFile.getName());
                        String contentType = preserve ? detactedContentType : format.equalsIgnoreCase("webp") ? "image/webp" : detactedContentType;

                        return FileUtil.fromFileToStream(resizedFile, contentType);
		} catch (FileNotFoundException fe) {
			logger.error(fe.getMessage());
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	public Response getRawResource(String directory, String fileName) {
		try {
			String inputFile = storageBasePath + File.separatorChar + directory + File.separatorChar + fileName;
			File file = AppUtil.findFile(inputFile);
			if (file == null) {
                            return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}
			Tika tika = new Tika();
			String contentType = tika.detect(file.getName());
                        return FileUtil.fromFileToStream(file, contentType);
		} catch (FileNotFoundException fe) {
			logger.error(fe.getMessage());
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	public Response getLogo(String directory, String fileName, Integer width, Integer height) {
		try {

			String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar;
			String fileLocation = dirPath + fileName;
			File file = AppUtil.findFile(fileLocation);

			if (file == null) {
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}
			logger.info("[files-api] File Location: {}.", fileLocation);

			String name = file.getName();

			String extension = name.substring(name.indexOf(".") + 1);
			String thumbnailFolder = storageBasePath + File.separatorChar + BASE_FOLDERS.thumbnails.getFolder()
					+ file.getParentFile().getAbsolutePath().substring(storageBasePath.length());
			String command = null;
			command = AppUtil.generateCommandLogo(file.getAbsolutePath(), thumbnailFolder, width, height, extension);
			logger.info("[files-api] Command: {}.", command);
			File thumbnailFile = AppUtil.getResizedImage(command);
			File resizedFile;
			Tika tika = new Tika();
			if (!thumbnailFile.exists()) {
                            File folders = new File(thumbnailFolder);
                            folders.mkdirs();
                            boolean fileGenerated = AppUtil.generateFile(command);
                            logger.info("[files-api] Generated? {}.", fileGenerated);
                            resizedFile = fileGenerated ? AppUtil.getResizedImage(command) : new File(file.toURI());
                            logger.info("[files-api] Resized? {}.", resizedFile);
			} else {
                            logger.info("[files-api] File Exists: {}.", thumbnailFile.getName());
                            resizedFile = thumbnailFile;
			}
			String contentType = tika.detect(resizedFile.getName());

                        return FileUtil.fromFileToStream(resizedFile, contentType);
		} catch (FileNotFoundException fe) {
			logger.error(fe.getMessage());
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
