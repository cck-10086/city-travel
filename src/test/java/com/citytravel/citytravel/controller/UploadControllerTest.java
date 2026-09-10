package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UploadControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectNonImageFile() {
        UploadController controller = createController();
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file);

        assertThat(result.getCode()).isEqualTo(400);
    }

    @Test
    void shouldStoreValidImageOutsideSourceDirectory() throws Exception {
        UploadController controller = createController();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).startsWith("/images/");
        String storedFilename = result.getData().substring("/images/".length());
        assertThat(Files.exists(tempDir.resolve(storedFilename))).isTrue();
    }

    private UploadController createController() {
        UploadController controller = new UploadController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        controller.init();
        return controller;
    }
}
