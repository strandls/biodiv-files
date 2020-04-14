package com.strandls.file.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.strandls.file.model.FileMetaData;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.util.ImageUtil.BASE_FOLDERS;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;

public class FileUploadService {
	
	private static final int GALLERY_WIDTH     = 500;
	private static final int GALLERY_HEIGHT    = 300;
	private static final int GALLERY_TH_WIDTH  = 50;
	private static final int GALLERY_TH_HEIGHT = 50;
	
	private static final int TH1_WIDTH  = 200;
	private static final int TH1_HEIGHT = 200;
	private static final int TH2_WIDTH  = 320;
	private static final int TH2_HEIGHT = 320;
	
	@Inject
	private FileMetaDataService fileMetaDataService;
	
	String storageBasePath = null;
	
	public FileUploadService() {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		storageBasePath = properties.getProperty("storage_dir", "/home/apps/biodiv-image");
	}
	
	public List<FileUploadModel> uploadMultipleFiles(String directory, FormDataBodyPart body, HttpServletRequest request, String hashKey) throws IOException {

		List<FileUploadModel> mutipleFiles = new ArrayList<FileUploadModel>();
		for(BodyPart part : body.getParent().getBodyParts()) {
			InputStream is = part.getEntityAs(InputStream.class);
			ContentDisposition contentDisposition = part.getContentDisposition();
			FileUploadModel file = uploadFile(directory, is, (FormDataContentDisposition) contentDisposition, request, hashKey);
			if(hashKey == null || "".equals(hashKey))
				hashKey = file.getHashKey();
			mutipleFiles.add(file);
		}
		return mutipleFiles;
	}

    public FileUploadModel uploadFile(String directory, InputStream inputStream, FormDataContentDisposition fileDetails,
            HttpServletRequest request, String hashKey) throws IOException {
    	
    	FileUploadModel fileUploadModel = new FileUploadModel();
    	
        String fileName = fileDetails.getFileName();
        
        String fileExtension = Files.getFileExtension(fileName);
        
        String folderName = "".equals(hashKey) ? UUID.randomUUID().toString() : hashKey;
        String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar + folderName; 
        
        String probeContentType = URLConnection.guessContentTypeFromName(fileName);
        
        if(probeContentType == null || !probeContentType.startsWith("image") && !probeContentType.startsWith("audio") && !probeContentType.startsWith("video")) {
        	fileUploadModel.setError("Invalid file type. Allowed types are image, audio and video");
        	return fileUploadModel;
        } else {
        	fileUploadModel.setType(probeContentType);
        }
        
        if("".equals(hashKey)) {
        	File dir = new File(dirPath);
        	boolean created = dir.mkdir();
        	if(!created) {
        		fileUploadModel.setError("Directory creation failed");
        		return fileUploadModel;
        	}
        }
        
        FileMetaData fileMetaData = new FileMetaData();
        fileMetaData.setFileName(fileName);
        fileMetaData.setPath(folderName);
        fileMetaDataService.save(fileMetaData);
        
        String generatedFileName = fileMetaData.getId() + "." + fileExtension;

        String filePath = dirPath + File.separatorChar + generatedFileName;

        boolean uploaded = writeToFile(inputStream, filePath);

        fileUploadModel.setUploaded(uploaded);

        if(probeContentType.startsWith("image"))
        	generateMultipleFiles(filePath, dirPath, fileMetaData.getId(),  fileExtension);
        
        if (uploaded) {
        	String resultPath = File.separatorChar + folderName + File.separatorChar + generatedFileName;
            fileUploadModel.setHashKey(folderName);
            fileUploadModel.setFileName(generatedFileName);
            fileUploadModel.setUri(resultPath);
            return fileUploadModel;
        } else {
        	fileUploadModel.setError("Unable to upload image");
        	return fileUploadModel;
        }
    }
    
    private FileUploadModel uploadFile(String directory, InputStream inputStream, String hashKey, String fileName) throws IOException {
    	
    	FileUploadModel fileUploadModel = new FileUploadModel();
        
        String fileExtension = Files.getFileExtension(fileName);
        
        String folderName = "".equals(hashKey) ? UUID.randomUUID().toString() : hashKey;
        String dirPath = storageBasePath + File.separatorChar + directory + File.separatorChar + folderName; 
        
        String probeContentType = URLConnection.guessContentTypeFromName(fileName);
        
        if(probeContentType == null || !probeContentType.startsWith("image") && !probeContentType.startsWith("audio") && !probeContentType.startsWith("video")) {
        	fileUploadModel.setError("Invalid file type. Allowed types are image, audio and video");
        	return fileUploadModel;
        } else {
        	fileUploadModel.setType(probeContentType);
        }
        
        if("".equals(hashKey)) {
        	File dir = new File(dirPath);
        	boolean created = dir.mkdir();
        	if(!created) {
        		fileUploadModel.setError("Directory creation failed");
        		return fileUploadModel;
        	}
        }
        
        FileMetaData fileMetaData = new FileMetaData();
        fileMetaData.setFileName(fileName);
        fileMetaData.setPath(folderName);
        fileMetaDataService.save(fileMetaData);
        
        String generatedFileName = fileMetaData.getId() + "." + fileExtension;

        String filePath = dirPath + File.separatorChar + generatedFileName;

        boolean uploaded = writeToFile(inputStream, filePath);

        fileUploadModel.setUploaded(uploaded);

        if(probeContentType.startsWith("image"))
        	generateMultipleFiles(filePath, dirPath, fileMetaData.getId(),  fileExtension);
        
        if (uploaded) {
        	String resultPath = File.separatorChar + folderName + File.separatorChar + generatedFileName;
            fileUploadModel.setHashKey(folderName);
            fileUploadModel.setFileName(generatedFileName);
            fileUploadModel.setUri(resultPath);
            return fileUploadModel;
        } else {
        	fileUploadModel.setError("Unable to upload image");
        	return fileUploadModel;
        }
    }
    
    public FileUploadModel saveFile(InputStream is, FormDataContentDisposition fileDetails, Long userId) {
    	String dir = storageBasePath + File.separatorChar + BASE_FOLDERS.myUploads.toString() + File.separatorChar + userId;
    	File dirFile = new File(dir);
    	if (!dirFile.exists()) {
    		dirFile.mkdir();
    	}
    	String fileName = dir + File.separatorChar + fileDetails.getFileName();
    	File file = new File(fileName);
    	boolean isFileCreated = writeToFile(is, file.getName());
    	FileUploadModel uploadModel = new FileUploadModel();
    	if (isFileCreated) {
            String probeContentType = URLConnection.guessContentTypeFromName(fileName);
    		uploadModel.setFileName(file.getName());
    		uploadModel.setUri(file.getName());
    		uploadModel.setType(probeContentType);
    		uploadModel.setUploaded(true);
    	} else {
    		uploadModel.setUploaded(false);
    		uploadModel.setError("Unable to upload image");
    	}
    	return uploadModel;
    }
    
    public List<FileUploadModel> getFilesFromUploads(Long userId) {
    	List<FileUploadModel> files = new ArrayList<>();
    	String userDir = BASE_FOLDERS.myUploads.toString() + File.separatorChar + userId;
    	try {
    		List<FileUploadModel> filesList = java.nio.file.Files.list(java.nio.file.Paths.get(storageBasePath + File.separatorChar + userDir)).map(f -> {
    			File tmpFile = f.toFile();
                String probeContentType = URLConnection.guessContentTypeFromName(tmpFile.getName());
    			FileUploadModel uploadModel = new FileUploadModel();
    			uploadModel.setFileName(tmpFile.getName());
    			uploadModel.setUri(userDir + File.separatorChar + tmpFile.getName());
    			uploadModel.setType(probeContentType);
    			return uploadModel;
    		}).collect(Collectors.toList());
    		
    		files.addAll(filesList);
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    	return files;
    }
    
    public List<FileUploadModel> moveFilesFromUploads(List<String> fileList) throws Exception {
    	List<FileUploadModel> files = new ArrayList<>();
    	for (String file: fileList) {
    		InputStream is = new FileInputStream(new File(storageBasePath + File.separatorChar + file));
    		String fileName = file.substring(file.lastIndexOf(File.separatorChar) + 1);
        	FileUploadModel model = uploadFile("observation", is, "", fileName);
        	files.add(model);
    	}
    	return files;
    }

	private boolean writeToFile(InputStream inputStream, String fileLocation) {
        try {
            OutputStream out = new FileOutputStream(new File(fileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(fileLocation));
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

	private void generateMultipleFiles(String filePath, String dirPath, long fileId, String extension) throws IOException {
		File file = new File(filePath);
		
		BufferedImage image = ImageIO.read(file);
		
		// If unable to parse the image file
		if(image == null) {
			return;
		}
		
		// Generate the gallery image.
		generateGallaryImage(image, dirPath, fileId, extension);

		// crop the image to square size
		int h = image.getHeight();
		int w = image.getWidth();
		int x = 0;
		int y = 0;
		if( w > h ) {
			x = (w-h) / 2;
			w = h;
		} else if( h > w ) {
			y = (h-w) / 2;
			h = w;
 		}
		BufferedImage cropedImage = image.getSubimage(x, y, w, h);
		
		// Generate Thumb nail images
		generateGalleryThumbnailImage(cropedImage, dirPath, fileId, extension);
		generateThumbnail1Image(cropedImage, dirPath, fileId, extension);
		generateThumbnail2Image(cropedImage, dirPath, fileId, extension);
	}
	
	public void generateMultipleFiles(String filePath, long fileId, String extension) throws IOException {
		File file = new File(filePath);
		
		BufferedImage image = ImageIO.read(file);
		
		// If unable to parse the image file
		if(image == null) {
			return;
		}

		// crop the image to square size
		int h = image.getHeight();
		int w = image.getWidth();
		int x = 0;
		int y = 0;
		if( w > h ) {
			x = (w-h) / 2;
			w = h;
		} else if( h > w ) {
			y = (h-w) / 2;
			h = w;
 		}
		BufferedImage cropedImage = image.getSubimage(x, y, w, h);
		
		generateThumbnail1Image(cropedImage, filePath, fileId, extension);
	}
	
	private void generateGallaryImage(BufferedImage image, String dirPath, long fileId, String extension) throws IOException {
		int h = image.getHeight();
		int w = image.getWidth();
		double hRatio = ((double) h)/GALLERY_HEIGHT;
		double wRatio = ((double) w)/GALLERY_WIDTH;
		int gall_w = 0, gall_h = 0;
		if(hRatio > wRatio) {
			gall_w = (int) (w/hRatio);
			gall_h = GALLERY_HEIGHT;
		} else {
			gall_w = GALLERY_WIDTH;
			gall_h = (int) (h/wRatio);
		}
		BufferedImage galleryImage = getScaledImage(image, gall_w, gall_h);
		File output = new File(dirPath + File.separatorChar + fileId + "_gall." + extension);
        ImageIO.write(galleryImage, extension, output);
	}
	
	private void generateGalleryThumbnailImage(BufferedImage cropedImage, String dirPath, long fileId,
			String extension) throws IOException {
		BufferedImage image = getScaledImage(cropedImage, GALLERY_TH_WIDTH, GALLERY_TH_HEIGHT);
		File output = new File(dirPath + File.separatorChar + fileId + "_gall_th." + extension);
        ImageIO.write(image, extension, output);
	}
	
	private void generateThumbnail1Image(BufferedImage cropedImage, String dirPath, long fileId,
			String extension) throws IOException {
		BufferedImage image = getScaledImage(cropedImage, TH1_WIDTH, TH1_HEIGHT);
		File output = new File(dirPath + File.separatorChar + fileId + "_th1." + extension);
        ImageIO.write(image, extension, output);
	}
	
	private void generateThumbnail2Image(BufferedImage cropedImage, String dirPath, long fileId,
			String extension) throws IOException {
		BufferedImage image = getScaledImage(cropedImage, TH2_WIDTH, TH2_HEIGHT);
		File output = new File(dirPath + File.separatorChar + fileId + "_th2." + extension);
        ImageIO.write(image, extension, output);
	}

	public static BufferedImage getScaledImage(BufferedImage image, int w, int h) {
		Image scaledImage = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
		BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resizedImage.createGraphics();
		g2.drawImage(scaledImage, 0, 0, null);
		g2.dispose();
		return resizedImage;
	}

	public static BufferedImage getScaledImage(BufferedImage image, int w, int h, int rgbType) {
		Image scaledImage = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
		BufferedImage resizedImage = new BufferedImage(w, h, rgbType);
		Graphics2D g2 = resizedImage.createGraphics();
		g2.drawImage(scaledImage, 0, 0, null);
		g2.dispose();
		return resizedImage;
	}
}
