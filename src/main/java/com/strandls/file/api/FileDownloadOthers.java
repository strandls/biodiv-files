package com.strandls.file.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import javax.inject.Inject;

import com.strandls.file.ApiContants;
import com.strandls.file.model.FileDownloadCredentials;
import com.strandls.file.service.FileAccessService;

@Path(ApiContants.DOWNLOAD)
public class FileDownloadOthers {

	@Inject
	private FileAccessService accessService;

	@Path("file")
	@GET
	public Response getFile(@QueryParam("accessKey") String accessKey) {
		try {
			FileDownloadCredentials credentials = accessService.getCredentials(accessKey);
			if (credentials != null) {
				return accessService.downloadFile(credentials);				
			}
			return Response.status(Status.FORBIDDEN).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}
	
	@Path(value = "/requestfile")
	@GET
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
