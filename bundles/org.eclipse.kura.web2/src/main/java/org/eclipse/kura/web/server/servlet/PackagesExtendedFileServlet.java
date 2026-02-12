package org.eclipse.kura.web.server.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtSupportedFeatures;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
    This class extends the FileServlet to add support for package deployment operations, which require the installation of the kura-deployment addon.
*/
public class PackagesExtendedFileServlet extends FileServlet {

    public PackagesExtendedFileServlet(final GwtSupportedFeatures supportedFeatures) {
        super(supportedFeatures);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        String reqPathInfo = req.getPathInfo();
        if (reqPathInfo == null) {
            logger.error(REQUEST_PATH_INFO_NOT_FOUND);
            throw new ServletException(REQUEST_PATH_INFO_NOT_FOUND);
        }

        super.doPost(req, resp);

        if (reqPathInfo.startsWith("/deploy")) {
            KuraRemoteServiceServlet.requirePermissions(req, Mode.ALL, new String[] { KuraPermission.PACKAGES_ADMIN });
            doPostDeploy(req);
        } else {
            super.doPost(req, resp);
        }

    }

    private void doPostDeploy(HttpServletRequest req) throws ServletException, IOException {

        ServiceLocator locator = ServiceLocator.getInstance();
        DeploymentAgentService deploymentAgentService;
        try {
            deploymentAgentService = locator.getService(DeploymentAgentService.class);
        } catch (GwtKuraException e) {
            logger.error("Error locating DeploymentAgentService");
            throw new ServletException("Error locating DeploymentAgentService", e);
        }

        HttpSession session = req.getSession(false);

        String reqPathInfo = req.getPathInfo();
        if (reqPathInfo.endsWith("url")) {

            String packageDownloadUrl = req.getParameter("packageUrl");
            if (packageDownloadUrl == null) {
                logger.error("Deployment package URL parameter missing");
                throw new ServletException("Deployment package URL parameter missing");
            }

            // BEGIN XSRF - Servlet dependent code
            String tokenId = req.getParameter(XSRF_TOKEN);

            try {
                GwtXSRFToken token = new GwtXSRFToken(tokenId);
                KuraRemoteServiceServlet.checkXSRFToken(req, token);
            } catch (Exception e) {
                throw new ServletException("Security error: please retry this operation correctly.", e);
            }
            // END XSRF security check

            try {
                logger.info("Installing package...");
                deploymentAgentService.installDeploymentPackageAsync(packageDownloadUrl);
            } catch (Exception e) {
                logger.error("Failed to install package at URL {}", packageDownloadUrl);
                throw new ServletException("Error installing deployment package", e);
            }

        } else if (reqPathInfo.endsWith("upload")) {
            doPostDeployUpload(req, session);
        } else {
            logger.error("Unsupported package deployment request");
            throw new ServletException("Unsupported package deployment request");
        }
    }

    private void doPostDeployUpload(HttpServletRequest req, HttpSession session) throws ServletException, IOException {
        ServiceLocator locator = ServiceLocator.getInstance();
        DeploymentAgentService deploymentAgentService;
        try {
            deploymentAgentService = locator.getService(DeploymentAgentService.class);
        } catch (GwtKuraException e) {
            logger.error("Error locating DeploymentAgentService");
            throw new ServletException("Error locating DeploymentAgentService", e);
        }

        // Check that we have a file upload request
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(req);
        if (!isMultipart) {
            logger.error("Not a file upload request");
            throw new ServletException("Not a file upload request");
        }

        UploadRequest upload = new UploadRequest(this.diskFileItemFactory);

        try {
            upload.parse(req);
        } catch (FileUploadException e) {
            logger.error(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
            throw new ServletException(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST, e);
        }

        // BEGIN XSRF - Servlet dependent code
        Map<String, String> formFields = upload.getFormFields();

        try {
            GwtXSRFToken token = new GwtXSRFToken(formFields.get(XSRF_TOKEN));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        List<DiskFileItem> fileItems = null;
        InputStream is = null;
        File localFile = null;
        OutputStream os = null;
        boolean successful = false;

        try {
            fileItems = upload.getFileItems();

            int fileItemsSize = fileItems.size();
            if (fileItemsSize != 1) {
                logger.error(EXPECTED_1_FILE_PATTERN, fileItemsSize);
                throw new ServletException("Wrong number of file items");
            }

            DiskFileItem item = fileItems.get(0);
            String filename = item.getName();
            is = item.getInputStream();

            String filePath = System.getProperty(JAVA_IO_TMPDIR) + File.separator + UUID.randomUUID() + ".dp";

            localFile = new File(filePath);
            if (localFile.exists() && !localFile.delete()) {
                logger.error("Cannot delete file: {}", filePath);
                throw new ServletException("Cannot delete file: " + filePath);
            }

            try {
                localFile.createNewFile();
                localFile.deleteOnExit();
            } catch (IOException e) {
                logger.error("Cannot create file: {}", filePath);
                throw new ServletException("Cannot create file: " + filePath);
            }

            try {
                os = new FileOutputStream(localFile);
            } catch (FileNotFoundException e) {
                logger.error("Cannot find file: {}", filePath);
                throw new ServletException("Cannot find file: " + filePath);
            }

            logger.info("Copying uploaded package file to file: {}", filePath);

            try {
                IOUtils.copy(is, os);
            } catch (IOException e) {
                logger.error("Failed to copy deployment package file: {}", filename);
                throw new ServletException("Failed to copy deployment package file: " + filename);
            }

            try {
                os.close();
            } catch (IOException e) {
                logger.warn(CANNOT_CLOSE_OUTPUT_STREAM, e);
            }

            URL url = localFile.toURI().toURL();
            String sUrl = url.toString();

            logger.info("Installing package...");
            try {
                deploymentAgentService.installDeploymentPackageAsync(sUrl);
                successful = true;
            } catch (Exception e) {
                logger.error("Package installation failed");
                throw new ServletException("Package installation failed", e);
            }

        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_OUTPUT_STREAM, e);
                }
            }
            if (localFile != null && !successful) {
                try {
                    localFile.delete();
                } catch (Exception e) {
                    logger.warn("Cannot delete file");
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_INPUT_STREAM, e);
                }
            }
            if (fileItems != null) {
                for (DiskFileItem fileItem : fileItems) {
                    fileItem.delete();
                }
            }
        }
    }

}
