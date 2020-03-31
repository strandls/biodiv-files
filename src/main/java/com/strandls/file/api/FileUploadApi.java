package com.strandls.file.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import com.strandls.file.ApiContants;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.service.FileUploadService;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.ImageUtil;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(ApiContants.UPLOAD)
@Api("Upload")
public class FileUploadApi {

	@Inject
	private FileUploadService fileUploadService;

	@POST
	@Path("multiple/{directory}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "upload the files to server", response = Map.class)
	public Response uploadMultiple(
			@Context HttpServletRequest request, 
			@FormDataParam("upload") FormDataBodyPart body,
			@PathParam("directory") String directory,
			@DefaultValue("") @QueryParam("hashKey") String hashKey) throws IOException {
		
		if (directory.contains("..") || hashKey.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		if (!ImageUtil.checkFolderExistence(directory)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		List<FileUploadModel> mutipleFiles = fileUploadService.uploadMultipleFiles(directory, body, request, hashKey);
		return Response.ok(mutipleFiles).build();
	}

	@POST
	@Path("/{directory}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "upload the file to server", response = Map.class)
	public Response uploadFile(
			@Context HttpServletRequest request, 
			@FormDataParam("upload") InputStream inputStream,
			@PathParam("directory") String directory,
			@FormDataParam("upload") FormDataContentDisposition fileDetails,
			@DefaultValue("") @QueryParam("hashKey") String hashKey) throws IOException {
		
		if (directory.contains("..") || hashKey.contains("..")) {
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		if (!ImageUtil.checkFolderExistence(directory)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		FileUploadModel uploadedFile = fileUploadService.uploadFile(directory, inputStream, fileDetails, request, hashKey);
		return Response.ok(uploadedFile).build();
	}
	
	@GET
	@Path(ApiContants.DWCFILE)
	@Produces(MediaType.TEXT_PLAIN)

	@ApiOperation(value = "Mapping of Document", notes = "Returns Document", response = Response.class)
	@ApiResponses(value = { @ApiResponse(code = 500, message = "ERROR", response = String.class) })

	public Response createDwcFILE() throws Exception {
		// folder where script is placed
		String filePath= "/app/configurations/scripts/";
		// folder where the new file will be placed
		String csvFilePath = "/app/data/biodiv/data-archive/gbif/" + AppUtil.getDatePrefix() + "dWC.csv";
		String script = "gbif_dwc.sh";
		try {
			Process process = Runtime.getRuntime().exec("sh " + script+" "+csvFilePath, null, new File(filePath));
			int exitCode = process.waitFor();
			if(exitCode == 0)
				return Response.status(Status.OK).entity("File Creation Successful!").build();
		}
		catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build(); 
		}
		return Response.status(Status.BAD_REQUEST).entity("File Creation Failed").build(); 
	}
	
}
