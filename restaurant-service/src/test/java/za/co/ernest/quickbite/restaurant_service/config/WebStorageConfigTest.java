package za.co.ernest.quickbite.restaurant_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebStorageConfigTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("addResourceHandlers - should map /uploads/** to normalized absolute file URI")
    void addResourceHandlers_RegistersUploadPathCorrectly() {
        WebStorageConfig config = new WebStorageConfig();
        ReflectionTestUtils.setField(config, "uploadDir", tempDir.toString());

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);

        config.addResourceHandlers(registry);

        String expectedUri = tempDir.toAbsolutePath().toUri().toString();
        verify(registry).addResourceHandler("/uploads/**");
        verify(registration).addResourceLocations(expectedUri);
    }

    @Test
    @DisplayName("addResourceHandlers - should resolve relative directory path to an absolute file URI")
    void addResourceHandlers_RelativePath_ConvertsToAbsoluteUri() {
        WebStorageConfig config = new WebStorageConfig();
        ReflectionTestUtils.setField(config, "uploadDir", "custom-uploads");

        StaticWebApplicationContext appContext = new StaticWebApplicationContext();
        appContext.setServletContext(new MockServletContext());
        ResourceHandlerRegistry realRegistry = new ResourceHandlerRegistry(appContext, appContext.getServletContext());

        assertDoesNotThrow(() -> config.addResourceHandlers(realRegistry));
        assertTrue(realRegistry.hasMappingForPattern("/uploads/**"));
    }
}

