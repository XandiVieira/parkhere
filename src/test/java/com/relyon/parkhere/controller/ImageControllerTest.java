package com.relyon.parkhere.controller;

import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.exception.ImageNotFoundException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.ImageStorageService;
import com.relyon.parkhere.service.LocalizedMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import(SecurityConfig.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void getImage_shouldReturn200() throws Exception {
        var user = buildUser();
        var resource = new ByteArrayResource("fake-image-data".getBytes());
        when(imageStorageService.load("test-image.jpg")).thenReturn(resource);

        mockMvc.perform(get("/api/v1/images/test-image.jpg")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk());
    }

    @Test
    void getImage_shouldReturn404WhenNotFound() throws Exception {
        var user = buildUser();
        when(imageStorageService.load("nonexistent.jpg")).thenThrow(new ImageNotFoundException("nonexistent.jpg"));
        when(localizedMessageService.translate(any(ImageNotFoundException.class))).thenReturn("Image not found");

        mockMvc.perform(get("/api/v1/images/nonexistent.jpg")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isNotFound());
    }
}
