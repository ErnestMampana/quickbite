package za.co.ernest.quickbite.restaurant_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import za.co.ernest.quickbite.restaurant_service.storage.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final Path rootLocation;
    private final String baseUrl;

    public LocalFileStorageService(
            @Value("${app.storage.local-dir:uploads}") String uploadDir,
            @Value("${app.storage.public-base-url:http://localhost:8080}") String baseUrl) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded image file cannot be empty.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and WEBP images are allowed.");
        }

        try {
            Path targetDir = rootLocation.resolve(folder).normalize();
            Files.createDirectories(targetDir);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (extension != null ? "." + extension : ".jpg");
            Path destinationFile = targetDir.resolve(filename);

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored image locally at [{}]", destinationFile);

            return baseUrl + "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store image file", e);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty() || files.size() > 5) {
            throw new IllegalArgumentException("A menu item must have between 1 and 5 images.");
        }
        return files.stream()
                .map(file -> uploadFile(file, folder))
                .toList();
    }

    @Override
    public void deleteFile(String fileUrl) {

        try {
            String relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + "/uploads/".length());
            Files.deleteIfExists(rootLocation.resolve(relativePath).normalize());
        } catch (Exception e) {
            log.warn("Could not delete local file [{}]: {}", fileUrl, e.getMessage());
        }
    }
}
