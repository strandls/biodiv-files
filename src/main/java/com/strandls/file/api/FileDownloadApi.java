package com.strandls.file.api;

import com.strandls.file.ApiContants;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.service.FileAccessService;
import com.strandls.file.service.FileDownloadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

@Path(ApiContants.GET)
@Api("Download")
public class FileDownloadApi {

	@Inject
	private FileDownloadService fileDownloadService;
	@Inject
	private FileAccessService accessService;
	
	@Path("ping")
	@GET
	@ApiOperation(value = "Dummy URI to print FileMetaData model", response = String.class)
	public Response ping() {
		return Response.status(Status.OK).entity("pong").build();
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
		if (directory == null || directory.isEmpty() || fileName == null || fileName.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (directory.contains("..") || fileName.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		String hAccept = request.getHeader(HttpHeaders.ACCEPT);
		boolean preserveFormat = Boolean.parseBoolean(presereve);
		String userRequestedFormat = 
				hAccept.contains("webp") && 
				format.equalsIgnoreCase("webp") ? 
						"webp" : !format.equalsIgnoreCase("webp") ? format : "jpg";
		return fileDownloadService.getImage(directory, fileName, width, height, userRequestedFormat, fit, preserveFormat);
	}
	
	@Path("raw/{directory:.+}/{fileName}")
	@GET
	@ApiOperation(value = "Get the raw resource", response = StreamingOutput.class)
	public Response getRawResource(@PathParam("directory") String directory,
			@PathParam("fileName") String fileName) throws Exception {
		fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
		if (directory == null || directory.isEmpty() || fileName == null || fileName.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (directory.contains("..") || fileName.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		return fileDownloadService.getRawResource(directory, fileName);
	}

	@Path(ApiContants.LOGO + "/{directory:.+}/{fileName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the image resource with custom height & width by url", response = StreamingOutput.class)
	public Response getUserGroupLogo(@Context HttpServletRequest request, @PathParam("directory") String directory,
			@PathParam("fileName") String fileName, @QueryParam("w") Integer width, @QueryParam("h") Integer height) throws Exception {
		fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
		if (height == null && width == null) {
			return Response.status(Status.BAD_REQUEST).entity("Height or Width required").build();			
		}
		if (directory == null || directory.isEmpty() || fileName == null || fileName.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (directory.contains("..") || fileName.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		return fileDownloadService.getLogo(directory, fileName, width, height);
	}
	
	@GET
	@Path(value = ApiContants.DOWNLOAD+"/requestfile")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "", notes = "Returns Document", response = Response.class)
	@ApiResponses(value = { @ApiResponse(code = 500, message = "ERROR", response = String.class) })	
	public Response downloadFileGivenPath(@QueryParam("type")String fileType, 
			@QueryParam("name")String fileName) {
		String path = null;
		if(fileType.equalsIgnoreCase("observation")) {
			path = "/app/data/biodiv/data-archive/listpagecsv";
		}
			try {
				if(Files.exists(Paths.get(path+File.separator+fileName), new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}))
					return accessService.genericFileDownload(path+File.separator+fileName);
			} catch (IOException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();

			}
		return Response.status(Status.BAD_REQUEST).build();
	}
}
