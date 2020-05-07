package com.strandls.file.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.google.inject.Inject;
import com.strandls.file.ApiContants;
import com.strandls.file.model.FileMetaData;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.service.FileDownloadService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path(ApiContants.GET)
@Api("Download")
public class FileDownloadApi {

	@Inject
	private FileDownloadService fileDownloadService;
	
	@Path("ping")
	@GET
	@ApiOperation(value = "Dummy URI to print FileMetaData model", response = FileMetaData.class)
	public Response ping() {
		return Response.status(Status.OK).entity(new FileMetaData()).build();
	}
	
	@Path("model")
	@GET
	@ApiOperation(value = "Dummy URI to print FileUploadModel model", response = FileUploadModel.class)
	public Response model() {
		return Response.status(Status.OK).entity(new FileUploadModel()).build();
	}
	
	@Path("crop/{directory:.+}/{fileName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the image resource with custom height & width by url", response = StreamingOutput.class)
	public Response getImage(@Context HttpServletRequest request, @PathParam("directory") String directory,
			@PathParam("fileName") String fileName, @QueryParam("w") Integer width, @QueryParam("h") Integer height,
			@DefaultValue("webp") @QueryParam("fm") String format, @DefaultValue("") @QueryParam("fit") String fit,
			@DefaultValue("false") @QueryParam("preserve") String presereve) throws Exception {
		fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
		if (height == null && width == null) {
			return Response.status(Status.BAD_REQUEST).entity("Height or Width required").build();			
		}
		if (directory.contains("..") || fileName.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		if (directory == null || directory.isEmpty() || fileName == null || fileName.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();			
		}
		String hAccept = request.getHeader(HttpHeaders.ACCEPT);
		boolean preserveFormat = Boolean.parseBoolean(presereve);
		String userRequestedFormat = 
				hAccept.contains("webp") && 
				format.equalsIgnoreCase("webp") ? 
						"webp" : !format.equalsIgnoreCase("webp") ? format : "jpg";
		return fileDownloadService.getImage(request, directory, fileName, width, height, userRequestedFormat, fit, preserveFormat);
	}
	
	@Path("raw/{directory:.+}/{fileName}")
	@GET
	@ApiOperation(value = "Get the raw resource", response = StreamingOutput.class)
	public Response getRawResource(@PathParam("directory") String directory,
			@PathParam("fileName") String fileName) throws Exception {
		fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());		
		if (directory.contains("..") || fileName.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		if (directory == null || directory.isEmpty() || fileName == null || fileName.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();			
		}
		return fileDownloadService.getRawResource(directory, fileName);
	}
}
