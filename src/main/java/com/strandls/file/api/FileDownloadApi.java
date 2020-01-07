package com.strandls.file.api;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

@Path(ApiContants.DOWNLOAD)
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

	@Path("custom/{hashKey}/{fileName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the image by url", response = StreamingOutput.class)
	public Response getFile(@Context HttpServletRequest request, @PathParam("hashKey") String hashKey,
			@PathParam("fileName") String fileName,
			@DefaultValue(ApiContants.ORIGINAL) @QueryParam("imageVariation") String imageVariation)
			throws FileNotFoundException {
		return fileDownloadService.getFile(hashKey, fileName, imageVariation);
	}

	@Path("{hashKey}/{fileName}/{width}/{height}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the image by url", response = StreamingOutput.class)
	public Response getCustomFile(@Context HttpServletRequest request, @PathParam("hashKey") String hashKey,
			@PathParam("fileName") String fileName, @PathParam("width") int outputWidth,
			@PathParam("height") int outputHeight) throws IOException {
		return fileDownloadService.getCustomSizeFile(hashKey, fileName, outputWidth, outputHeight);
	}

	@Path("{directory}/{hashKey}/{fileName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the image resource with custom height & width by url", response = StreamingOutput.class)
	public Response getImageResource(@Context HttpServletRequest request, @PathParam("directory") String directory, @PathParam("hashKey") String hashKey,
			@PathParam("fileName") String fileName, @QueryParam("w") Integer width, @QueryParam("h") Integer height,
			@DefaultValue("webp") @QueryParam("fm") String format) throws Exception {
		String hAccept = request.getHeader(HttpHeaders.ACCEPT);
		String userRequestedFormat = 
				hAccept.contains("webp") && 
				format.equalsIgnoreCase("webp") ? 
						"webp" : !format.equalsIgnoreCase("webp") ? format : "jpg";
		return fileDownloadService.getImageResource(request, directory, hashKey, fileName, width, height, userRequestedFormat);
	}
}
