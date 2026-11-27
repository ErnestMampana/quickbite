package za.co.ernest.quickbite.restaurant_service.storage;


import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String folder);
    List<String> uploadFiles(List<MultipartFile> files, String folder);
    void deleteFile(String fileUrl);
}
