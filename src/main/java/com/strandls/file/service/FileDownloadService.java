package com.strandls.file.service;

import com.strandls.file.util.AppUtil;
import com.strandls.file.util.AppUtil.BASE_FOLDERS;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.Properties;

public class FileDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadService.class);

    private final String storageBasePath;

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

    public Response getImage(String directory, String fileName, Integer width, Integer height,
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
            String command;
            command = AppUtil.generateCommand(file.getAbsolutePath(), thumbnailFolder,
                    width, height, preserve ? extension : format, null, fit);
            File thumbnailFile = AppUtil.getResizedImage(command);
            File resizedFile;
            Tika tika = new Tika();
            if (!thumbnailFile.exists()) {
                File folders = new File(thumbnailFolder);
                boolean isCreated = folders.mkdirs();
                if (!isCreated) logger.debug("Directory creation failed: {}", folders.getAbsolutePath());
                boolean fileGenerated = AppUtil.generateFile(command);
                resizedFile = fileGenerated ? AppUtil.getResizedImage(command) : new File(file.toURI());
            } else {
                resizedFile = thumbnailFile;
            }
            String contentType = tika.detect(resizedFile.getName());
            InputStream in = new FileInputStream(resizedFile);
            long contentLength = resizedFile.length();
            StreamingOutput sout = out -> {
                byte[] buf = new byte[8192];
                int c;
                while ((c = in.read(buf, 0, buf.length)) > 0) {
                    out.write(buf, 0, c);
                    out.flush();
                }
                in.close();
                out.close();
            };
            return Response.ok(sout)
                    .type(preserve ? contentType : format.equalsIgnoreCase("webp") ? "image/webp" : contentType)
                    .header("Content-Length", contentLength).cacheControl(AppUtil.getCacheControl()).build();
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
            logger.error(fe.getMessage());
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response getRawResource(String directory, String fileName) {
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
            long contentLength = file.length();
            StreamingOutput sout = output -> {
                byte[] buf = new byte[8192];
                int c;
                while ((c = in.read(buf, 0, buf.length)) > 0) {
                    output.write(buf, 0, c);
                    output.flush();
                }
                in.close();
                output.close();
            };
            return Response.ok(sout).type(contentType).header("Content-Length", contentLength)
                    .cacheControl(AppUtil.getCacheControl()).build();
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
            System.out
                    .println("\n\n***** FileLocation: " + fileLocation + " ***** " + file.getCanonicalPath() + "\n\n");

            String name = file.getName();

            String extension = name.substring(name.indexOf(".") + 1);
            String thumbnailFolder = storageBasePath + File.separatorChar + BASE_FOLDERS.thumbnails.getFolder()
                    + file.getParentFile().getAbsolutePath().substring(storageBasePath.length());
            String command = AppUtil.generateCommandLogo(file.getAbsolutePath(), thumbnailFolder,
                    width, height, extension);
            logger.debug("***** Command: {} *****", command);
            File thumbnailFile = AppUtil.getResizedImage(command);
            File resizedFile;
            Tika tika = new Tika();
            if (!thumbnailFile.exists()) {
                File folders = new File(thumbnailFolder);
                boolean isCreated = folders.mkdirs();
                if (!isCreated) logger.debug("Directory creation failed: {}", folders.getAbsolutePath());
                boolean fileGenerated = AppUtil.generateFile(command);
                System.out.println("\n\n**** Generated? " + fileGenerated + " *****\n\n");
                resizedFile = fileGenerated ? AppUtil.getResizedImage(command) : new File(file.toURI());
                System.out.println("\n\n**** Resized? " + resizedFile + " *****\n\n");
            } else {
                System.out.println("\n\n**** File Exists: " + thumbnailFile.getName() + " *****\n\n");
                resizedFile = thumbnailFile;
            }
            String contentType = tika.detect(resizedFile.getName());
            InputStream in = new FileInputStream(resizedFile);
            long contentLength = resizedFile.length();
            StreamingOutput sout = out -> {
                byte[] buf = new byte[8192];
                int c;
                while ((c = in.read(buf, 0, buf.length)) > 0) {
                    out.write(buf, 0, c);
                    out.flush();
                }
                in.close();
                out.close();
            };
            return Response.ok(sout)
                    .type(contentType)
                    .header("Content-Length", contentLength).cacheControl(AppUtil.getCacheControl()).build();
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
            logger.error(fe.getMessage());
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
