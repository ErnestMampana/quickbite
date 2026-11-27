package za.co.ernest.quickbite.restaurant_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString(), BASE_URL);
    }

    @Test
    @DisplayName("uploadFile - should store valid JPEG file and return accessible URL")
    void uploadFile_ValidJpeg_StoresFileAndReturnsUrl() throws IOException {
        byte[] content = "fake-image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "images", "burger.jpg", "image/jpeg", content
        );

        String returnedUrl = storageService.uploadFile(file, "menu-items");

        assertNotNull(returnedUrl);
        assertTrue(returnedUrl.startsWith(BASE_URL + "/uploads/menu-items/"));
        assertTrue(returnedUrl.endsWith(".jpg"));

        String filename = returnedUrl.substring(returnedUrl.lastIndexOf('/') + 1);
        Path storedFile = tempDir.resolve("menu-items").resolve(filename);
        assertTrue(Files.exists(storedFile));
        assertArrayEquals(content, Files.readAllBytes(storedFile));
    }

    @Test
    @DisplayName("uploadFile - should default to .jpg extension when original filename has no extension")
    void uploadFile_NoExtension_DefaultsToJpg() {
        MockMultipartFile file = new MockMultipartFile(
                "images", "burger", "image/png", "png-bytes".getBytes()
        );

        String returnedUrl = storageService.uploadFile(file, "menu-items");

        assertTrue(returnedUrl.endsWith(".jpg"));
    }

    @Test
    @DisplayName("uploadFile - should throw IllegalArgumentException when file is null or empty")
    void uploadFile_NullOrEmpty_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "images", "empty.jpg", "image/jpeg", new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(null, "menu-items"));
        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(emptyFile, "menu-items"));
    }

    @Test
    @DisplayName("uploadFile - should reject disallowed content types like PDF or GIF")
    void uploadFile_InvalidContentType_ThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "images", "menu.pdf", "application/pdf", "pdf-bytes".getBytes()
        );
        MockMultipartFile nullTypeFile = new MockMultipartFile(
                "images", "unknown.bin", null, "raw-bytes".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(pdfFile, "menu-items"));
        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(nullTypeFile, "menu-items"));
    }

    @Test
    @DisplayName("uploadFile - should wrap IOException into IllegalStateException")
    void uploadFile_StreamFails_ThrowsIllegalStateException() throws IOException {
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getContentType()).thenReturn("image/webp");
        when(brokenFile.getOriginalFilename()).thenReturn("dish.webp");
        when(brokenFile.getInputStream()).thenThrow(new IOException("Disk I/O failure"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> storageService.uploadFile(brokenFile, "menu-items")
        );
        assertEquals("Failed to store image file", ex.getMessage());
    }

    @Test
    @DisplayName("uploadFiles - should store 1 to 5 valid images and return matching URLs")
    void uploadFiles_ValidBatch_ReturnsUrls() {
        List<MultipartFile> files = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "img1".getBytes()),
                new MockMultipartFile("images", "2.png", "image/png", "img2".getBytes()),
                new MockMultipartFile("images", "3.webp", "image/webp", "img3".getBytes())
        );

        List<String> urls = storageService.uploadFiles(files, "menu-items");

        assertEquals(3, urls.size());
        assertTrue(urls.get(0).endsWith(".jpg"));
        assertTrue(urls.get(1).endsWith(".png"));
        assertTrue(urls.get(2).endsWith(".webp"));
    }

    @Test
    @DisplayName("uploadFiles - should reject null, empty, or more than 5 files")
    void uploadFiles_InvalidCount_ThrowsException() {
        MultipartFile sample = new MockMultipartFile("images", "a.jpg", "image/jpeg", "img".getBytes());
        List<MultipartFile> sixFiles = List.of(sample, sample, sample, sample, sample, sample);

        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFiles(null, "menu-items"));
        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFiles(Collections.emptyList(), "menu-items"));
        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFiles(sixFiles, "menu-items"));
    }

    @Test
    @DisplayName("deleteFile - should delete existing file from disk using its public URL")
    void deleteFile_ExistingFile_DeletesFromDisk() {
        MockMultipartFile file = new MockMultipartFile(
                "images", "delete-me.jpg", "image/jpeg", "bytes".getBytes()
        );
        String url = storageService.uploadFile(file, "menu-items");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path storedPath = tempDir.resolve("menu-items").resolve(filename);

        assertTrue(Files.exists(storedPath));

        storageService.deleteFile(url);

        assertFalse(Files.exists(storedPath));
    }

    @Test
    @DisplayName("deleteFile - should safely swallow exceptions for malformed URLs without crashing")
    void deleteFile_MalformedUrl_DoesNotThrow() {
        assertDoesNotThrow(() -> storageService.deleteFile("invalid-url-without-uploads-segment"));
        assertDoesNotThrow(() -> storageService.deleteFile(null));
    }
}

