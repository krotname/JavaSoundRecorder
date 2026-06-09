package com.krotname.javasoundrecorder.storage;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.WriteMode;
import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Production Dropbox uploader. Upload destination and token come only from config.
 */
public final class DropboxUploadService implements UploadService {
    private static final String APP_NAME = "javasoundrecorder";
    private final String uploadPath;
    private final DbxClientV2 client;

    public DropboxUploadService(String accessToken, String uploadPath) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("DROPBOX_ACCESS_TOKEN is required when upload is enabled.");
        }
        this.uploadPath = uploadPath.endsWith("/") ? uploadPath.substring(0, uploadPath.length() - 1) : uploadPath;
        DbxRequestConfig config = DbxRequestConfig.newBuilder(APP_NAME).build();
        this.client = new DbxClientV2(config, accessToken);
    }

    /**
     * Uploads by opening a one-time read stream and overwriting existing remote file.
     * This keeps the adapter side-effect clear and avoids partial metadata reads.
     */
    @Override
    public FileUploadResult upload(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            String destination = uploadPath + "/" + file.getFileName();
            FileMetadata metadata = client.files()
                    .uploadBuilder(destination)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
            return new FileUploadResult(metadata.getPathDisplay(), metadata.getSize());
        } catch (UploadErrorException e) {
            throw new IOException("Dropbox upload path rejected: " + e.getMessage(), e);
        } catch (DbxException e) {
            throw new IOException("Dropbox call failed", e);
        }
    }
}
