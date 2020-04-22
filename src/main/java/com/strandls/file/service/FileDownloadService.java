package com.strandls.file.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.strandls.file.ApiContants;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.ImageUtil;
import com.strandls.file.util.ImageUtil.BASE_FOLDERS;
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
			e.printStackTrace();
		}

		storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv-image");
	}

	public Response getFile(String hashKey, String fileName, String imageVariation) throws FileNotFoundException {
		if (!ApiContants.ORIGINAL.equals(imageVariation)) {
			String extension = Files.getFileExtension(fileName);
			String fileNameWithoutExtension = Files.getNameWithoutExtension(fileName);
			fileName = fileNameWithoutExtension + "_" + imageVariation + "." + extension;
		}

		String fileLocation = storageBasePath + File.separatorChar + hashKey + File.separatorChar + fileName;

		InputStream in = new FileInputStream(new File(fileLocation));
		StreamingOutput sout;
		sout = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				byte[] buf = new byte[8192];
				int c;
				while ((c = in.read(buf, 0, buf.length)) > 0) {
					out.write(buf, 0, c);
					out.flush();
				}
				out.close();
			}
		};
		return Response.ok(sout).type("image/" + Files.getFileExtension(fileLocation))
				.cacheControl(AppUtil.getCacheControl()).build();
	}

	public Response getCustomSizeFile(String hashKey, String fileName, int outputWidth, int outputHeight)
			throws IOException {

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

		@SuppressWarnings("resource")
		InputStream in = new FileInputStream(output);
		StreamingOutput sout;
		sout = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				byte[] buf = new byte[8192];
				int c;
				while ((c = in.read(buf, 0, buf.length)) > 0) {
					out.write(buf, 0, c);
					out.flush();
				}
				out.close();
			}
		};
		return Response.ok(sout).type("image/" + Files.getFileExtension(fileLocation))
				.cacheControl(AppUtil.getCacheControl()).build();
	}

	public Response getImageResource(HttpServletRequest req, String directory, String fileName, Integer width,
			Integer height, String format) throws Exception {

		String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar;
		String fileLocation = dirPath + fileName;
//		File file = new File(fileLocation);
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
		InputStream in = new FileInputStream(isWebp ? webpOutput : output);
		StreamingOutput sout;
		sout = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				byte[] buf = new byte[8192];
				int c;
				while ((c = in.read(buf, 0, buf.length)) > 0) {
					out.write(buf, 0, c);
					out.flush();
				}
				in.close();
				out.close();
			}
		};
		return Response.ok(sout).type(isWebp ? "image/webp" : contentType).cacheControl(AppUtil.getCacheControl())
				.build();
	}

	public Response getImage(HttpServletRequest req, String directory, String fileName, Integer width, Integer height,
			String format) throws Exception {
		try {

			String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar;
			String fileLocation = dirPath + fileName;
			File file = AppUtil.findFile(fileLocation);

			if (file == null) {
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}

			String command = null;
			if (directory.startsWith(BASE_FOLDERS.myUploads.toString())) {
				command = AppUtil.generateCommand(file.getAbsolutePath(),
						storageBasePath + File.separatorChar + BASE_FOLDERS.thumbnails.toString(), width, height,
						format, null);
			} else {
				command = AppUtil.generateCommand(file.getAbsolutePath(), width, height, format, null);
			}

			Tika tika = new Tika();
			boolean fileGenerated = AppUtil.generateFile(command);
			File resizedFile = AppUtil.getResizedImage(fileGenerated ? command : file.getAbsolutePath());
			String contentType = tika.detect(resizedFile.getName());
			InputStream in = new FileInputStream(resizedFile);
			StreamingOutput sout;
			sout = new StreamingOutput() {
				@Override
				public void write(OutputStream out) throws IOException, WebApplicationException {
					byte[] buf = new byte[8192];
					int c;
					while ((c = in.read(buf, 0, buf.length)) > 0) {
						out.write(buf, 0, c);
						out.flush();
					}
					in.close();
					out.close();
				}
			};
			return Response.ok(sout).type(format.equalsIgnoreCase("webp") ? "image/webp" : contentType)
					.cacheControl(AppUtil.getCacheControl())
					.build();
		} catch (FileNotFoundException fe) {
			logger.error(fe.getMessage());
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	public Response getRawResource(String directory, String fileName) throws Exception {
		try {
			String inputFile = storageBasePath + File.separatorChar + directory + File.separatorChar + fileName;
//		File file = new File(inputFile);
			File file = AppUtil.findFile(inputFile);
			if (file == null) {
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}
			InputStream in = new FileInputStream(file.getAbsolutePath());
			Tika tika = new Tika();
			String contentType = tika.detect(file.getName());
			StreamingOutput sout;
			sout = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					byte[] buf = new byte[8192];
					int c;
					while ((c = in.read(buf, 0, buf.length)) > 0) {
						output.write(buf, 0, c);
						output.flush();
					}
					in.close();
					output.close();
				}
			};
			return Response.ok(sout).type(contentType).cacheControl(AppUtil.getCacheControl())
					.build();
		} catch (FileNotFoundException fe) {
			logger.error(fe.getMessage());
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
