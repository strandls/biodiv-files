package com.strandls.file.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import com.strandls.file.ApiContants;
import com.strandls.file.model.FileDownloadCredentials;
import com.strandls.file.model.FileDownloads;
import com.strandls.file.service.FileAccessService;

@Path(ApiContants.DOWNLOAD)
public class FileDownloadOthers {

	@Inject
	private FileAccessService accessService;

	@Path("file/{fileName}")
	@GET
	public Response getFile(@PathParam("fileName") String fileName, @QueryParam("accessKey") String accessKey) {
		try {
			FileDownloadCredentials credentials = accessService.getCredentials(accessKey);

			return accessService.downloadFile(credentials, fileName);
		} catch (Exception ex) {
			ex.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

}
