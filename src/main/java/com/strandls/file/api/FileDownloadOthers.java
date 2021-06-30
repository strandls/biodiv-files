package com.strandls.file.api;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.file.ApiContants;
import com.strandls.file.model.FileDownloadCredentials;
import com.strandls.file.service.FileAccessService;

@Path(ApiContants.DOWNLOAD)
public class FileDownloadOthers {
    
	private static final Logger logger = LoggerFactory.getLogger(FileDownloadOthers.class);

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
			 logger.error(ex.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

}
