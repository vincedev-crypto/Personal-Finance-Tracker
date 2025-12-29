package com.appdev.Finance;

import com.appdev.Finance.Service.FileStorageService;
import com.appdev.Finance.Service.TransactionService;
import com.appdev.Finance.model.TransactionFile;
import com.appdev.Finance.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition; // Import this
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam; // Import for query parameter
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Controller
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TransactionService transactionService;

    // List of MIME types that can generally be displayed inline by browsers
    private static final List<String> INLINE_VIEWABLE_TYPES = Arrays.asList(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.TEXT_PLAIN_VALUE
            // Add more viewable types like text/html, image/svg+xml etc. if needed
    );

    // Combined endpoint for both view and download, controlled by a query parameter
    @GetMapping("/transactions/files/{fileId}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable Long fileId,
                                              @RequestParam(name = "action", defaultValue = "download") String action, // "view" or "download"
                                              HttpServletRequest request,
                                              HttpSession session) {
        logger.info("Received request to {} file with ID: {}", action, fileId);
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            logger.warn("Unauthorized attempt to {} file ID: {}. User not logged in.", action, fileId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        logger.info("User {} attempting to {} file ID: {}", loggedInUser.getEmail(), action, fileId);

        TransactionFile transactionFile = transactionService.getTransactionFileByIdAndUser(fileId, loggedInUser);

        if (transactionFile == null) {
            logger.warn("TransactionFile not found or not owned by user {} for file ID: {}", loggedInUser.getEmail(), fileId);
            return ResponseEntity.notFound().build();
        }
        logger.info("TransactionFile found: ID={}, Original Name='{}', Stored Name='{}', ContentType='{}'",
            transactionFile.getId(),
            transactionFile.getOriginalFileName(),
            transactionFile.getStoredFileName(),
            transactionFile.getContentType());

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(transactionFile.getStoredFileName());
        } catch (Exception e) {
            logger.error("Error loading file as resource for stored name {}: {}", transactionFile.getStoredFileName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (!resource.exists() || !resource.isReadable()) {
            logger.error("Resource for stored file name {} is null, does not exist, or is not readable.", transactionFile.getStoredFileName());
            return ResponseEntity.notFound().build();
        }
        logger.info("Resource loaded: Filename='{}', Exists? {}, Readable? {}", resource.getFilename(), resource.exists(), resource.isReadable());

        String contentType = transactionFile.getContentType(); // Use the stored content type
        if (contentType == null || contentType.isBlank()) {
            // Fallback if content type wasn't stored properly
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                logger.info("Determined contentType from servlet context: {} for file: {}", contentType, resource.getFilename());
            } catch (IOException ex) {
                logger.warn("IOException determining content type for resource: {}. Falling back. Error: {}", resource.getFilename(), ex.getMessage());
            }
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Default fallback
                logger.info("ContentType defaulted to application/octet-stream for: {}", transactionFile.getOriginalFileName());
            }
        }


        ContentDisposition.Builder dispositionBuilder;
        if ("view".equalsIgnoreCase(action) && INLINE_VIEWABLE_TYPES.contains(contentType.toLowerCase())) {
            dispositionBuilder = ContentDisposition.inline();
            logger.info("Serving file ID {} for inline view.", fileId);
        } else {
            dispositionBuilder = ContentDisposition.attachment(); // Default to download for safety or if action is not 'view'
            logger.info("Serving file ID {} for download (action: {}, type: {}).", fileId, action, contentType);
        }

        String encodedFilename;
        try {
            // It's good practice to use the original filename for the user.
            encodedFilename = URLEncoder.encode(transactionFile.getOriginalFileName(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            logger.warn("Could not URL encode original filename: {}. Using a default name.", transactionFile.getOriginalFileName(), e);
            encodedFilename = "file_" + transactionFile.getStoredFileName(); // Fallback filename
        }
        
        ContentDisposition contentDisposition = dispositionBuilder.filename(encodedFilename, StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
}