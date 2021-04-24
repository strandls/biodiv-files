package com.strandls.file.api;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.pac4j.core.profile.CommonProfile;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.file.ApiContants;
import com.strandls.file.dto.FilesDTO;
import com.strandls.file.model.FileUploadModel;
import com.strandls.file.model.MyUpload;
import com.strandls.file.service.FileUploadService;
import com.strandls.file.util.AppUtil;
import com.strandls.file.util.AppUtil.BASE_FOLDERS;
import com.strandls.file.util.AppUtil.MODULE;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(ApiContants.UPLOAD)
@Api("Upload")
public class FileUploadApi {

	@Inject
	private FileUploadService fileUploadService;

	@POST
	@Path(ApiContants.MY_UPLOADS)
	@ValidateUser
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Upload files to myUploads", notes = "Returns uploaded file data", response = MyUpload.class)
	public Response saveToMyUploads(@Context HttpServletRequest request,
			@FormDataParam("upload") InputStream inputStream,
			@FormDataParam("upload") FormDataContentDisposition fileDetails, @FormDataParam("hash") String hash,
			@FormDataParam("module") String module) {
		if (hash == null || hash.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Hash required").build();
		}

		if (inputStream == null) {
			return Response.status(Status.BAD_REQUEST).entity("Input upload  Stream required").build();
		}
		
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			MODULE mod = AppUtil.getModule(module);
			if (mod == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid Module").build();
			}
			MyUpload uploadModel = fileUploadService.saveFile(inputStream, mod, fileDetails, hash, userId);
			return Response.ok().entity(uploadModel).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

	@GET
	@Path(ApiContants.MY_UPLOADS)
	@ValidateUser
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get files list from myUploads", notes = "Returns uploaded file data", response = MyUpload.class, responseContainer = "List")
	public Response getFilesFromUploads(@Context HttpServletRequest request, @QueryParam("module") String module) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			MODULE mod = AppUtil.getModule(module);
			if (mod == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid Module").build();
			}
			List<MyUpload> files = fileUploadService.getFilesFromUploads(userId, mod);
			return Response.ok().entity(files).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

	@GET
	@Path(ApiContants.DWCFILE)
	@Produces(MediaType.TEXT_PLAIN)

	@ApiOperation(value = "Mapping of Document", notes = "Returns Document", response = Response.class)
	@ApiResponses(value = { @ApiResponse(code = 500, message = "ERROR", response = String.class) })
	public Response createDwcFILE() {
		// folder where script is placed
		String filePath = "/app/configurations/scripts/";
		// folder where the new file will be placed
		String csvFilePath = "/app/data/biodiv/data-archive/gbif/" + AppUtil.getDatePrefix() + "dWC.csv";
		String script = "gbif_dwc.sh";
		try {
			Process process = Runtime.getRuntime().exec("sh " + script + " " + csvFilePath, null, new File(filePath));
			int exitCode = process.waitFor();
			if (exitCode == 0)
				return Response.status(Status.OK).entity("File Creation Successful!").build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return Response.status(Status.BAD_REQUEST).entity("File Creation Failed").build();
	}

	@POST
	@Path(ApiContants.MOVE_FILES)
	@ValidateUser
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Moves files from MyUploads to the appropriate folder", notes = "Returns uploaded file data", response = Map.class)
	public Response moveFiles(@Context HttpServletRequest request, @ApiParam("filesDTO") FilesDTO filesDTO) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			
			MODULE module = AppUtil.getModule(filesDTO.getModule() != null ? filesDTO.getModule() : null);
			if (module == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid Module").build();
			}
			Map<String, Object> files = fileUploadService.moveFilesFromUploads(userId, filesDTO.getFiles(),
					filesDTO.getFolder(),module);
			return Response.ok().entity(files).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

	@POST
	@Path(ApiContants.REMOVE_FILE)
	@ValidateUser
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Delete file from my-uploads folder", notes = "Returns if the file was deleted", response = String.class)
	public Response removeFile(@Context HttpServletRequest request, MyUpload file) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			boolean deleted = fileUploadService.deleteFilesFromMyUploads(userId, file.getPath());
			return Response.ok().entity(deleted).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

	@POST
	@Path(ApiContants.RESOURCE_UPLOAD)
	@ValidateUser
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Upload resources", notes = "Returns uploaded file data", response = FileUploadModel.class)
	public Response uploadResource(@Context HttpServletRequest request,
			@FormDataParam("upload") InputStream inputStream,
			@FormDataParam("upload") FormDataContentDisposition fileDetails,
			@DefaultValue("") @FormDataParam("hash") String hash, @FormDataParam("directory") String directory,
			@FormDataParam("nestedFolder") String nestedFolder,
			@DefaultValue("false") @FormDataParam("resource") String resource) {
		try {
			boolean createResourceFolder = Boolean.parseBoolean(resource);
			BASE_FOLDERS folder = AppUtil.getFolder(directory);
			if (folder == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid directory").build();
			}
			if (inputStream == null) {
				return Response.status(Status.BAD_REQUEST).entity("File required").build();
			}
			FileUploadModel model = fileUploadService.uploadFile(folder, inputStream, fileDetails, request,
					nestedFolder, hash, createResourceFolder);
			return Response.ok().entity(model).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

	@POST
	@Path(ApiContants.BULK_UPLOAD)
	@ValidateUser
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Bulk Upload", notes = "Returns uploaded file data", response = Map.class)
	public Response handleBulkUpload(@Context HttpServletRequest httpServletRequest,
			FormDataMultiPart formDataMultiPart) {
		try {
			FormDataBodyPart moduleBodyPart = formDataMultiPart.getField("module");
			MODULE module = AppUtil.getModule(moduleBodyPart != null ? moduleBodyPart.getValue() : null);
			if (module == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid Module").build();
			}
			FormDataBodyPart folderBodyPart = formDataMultiPart.getField("folder");
			BASE_FOLDERS folder = AppUtil.getFolder(folderBodyPart != null ? folderBodyPart.getValue() : null);
			if (folder == null) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid directory").build();
			}
			List<FormDataBodyPart> filesBodyPart = formDataMultiPart.getFields("upload");
			if (filesBodyPart == null || filesBodyPart.isEmpty()) {
				return Response.status(Status.BAD_REQUEST).entity("File(s) required").build();
			}
			Map<String, Object> response = new HashMap<>();
			List<MyUpload> files = fileUploadService.handleBulkUpload(httpServletRequest, module, filesBodyPart);
			response.put("status", !files.isEmpty());
			response.put("files", files);
			return Response.ok().entity(response).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}
	
	

	@POST
	@Path(ApiContants.BULK + ApiContants.FILES_PATH)
	@ValidateUser
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Provides cononical hash map list of all files from myUploads for a given userId and Module ", notes = "Returns uploaded file data", response = Map.class)
	public Response getAllFilePathsByUser(@Context HttpServletRequest request,
			@ApiParam("filesDTO") FilesDTO filesDTO) {
		
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			BASE_FOLDERS folder = AppUtil.getFolder(filesDTO.getFolder());
			if (folder == null) {
				throw new Exception("Invalid folder");
			}
			MODULE module = AppUtil.getModule(filesDTO.getModule());
			if (module == null) {
				throw new Exception("Invalid module");
			}
			Map<String, String> files = fileUploadService.getAllFilePathsByUser(userId, folder,
					module);
			return Response.ok().entity(files).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
		
	}
	

	@POST
	@Path(ApiContants.BULK + ApiContants.MOVE_FILES)
	@ValidateUser
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Bulk Upload - Moves files from MyUploads to the appropriate folder", notes = "Returns uploaded file data", response = Map.class)
	public Response handleBulkUploadMoveFiles(@Context HttpServletRequest request,
			@ApiParam("filesDTO") FilesDTO filesDTO) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			BASE_FOLDERS folder = AppUtil.getFolder(filesDTO.getFolder());
			if (folder == null) {
				throw new Exception("Invalid folder");
			}
			MODULE module = AppUtil.getModule(filesDTO.getModule());
			if (module == null) {
				throw new Exception("Invalid module");
			}
			Map<String, Object> files = fileUploadService.moveFilesFromUploads(userId, filesDTO.getFiles(), folder,
					module);
			return Response.ok().entity(files).build();
		} catch (Exception ex) {
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}

}
