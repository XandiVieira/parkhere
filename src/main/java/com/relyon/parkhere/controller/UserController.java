package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.ChangePasswordRequest;
import com.relyon.parkhere.dto.request.UpdatePreferencesRequest;
import com.relyon.parkhere.dto.request.UpdateUserRequest;
import com.relyon.parkhere.dto.response.GamificationResponse;
import com.relyon.parkhere.dto.response.PreferencesResponse;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.FavoriteService;
import com.relyon.parkhere.service.GamificationService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.PreferenceService;
import com.relyon.parkhere.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Users", description = "User profile, favorites, and gamification")
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;
    private final GamificationService gamificationService;
    private final PreferenceService preferenceService;
    private final LocalizedMessageService messageService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @PutMapping(value = "/me/profile-pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateProfilePic(@AuthenticationPrincipal User user,
                                                          @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateProfilePic(user, file));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal User user,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request);
        return ResponseEntity.ok(Map.of("message", messageService.translate("user.password.changed")));
    }

    @GetMapping("/me/favorites")
    public ResponseEntity<Page<SpotResponse>> getFavorites(@AuthenticationPrincipal User user,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(favoriteService.getFavorites(user, PageRequest.of(page, size)));
    }

    @GetMapping("/me/gamification")
    public ResponseEntity<GamificationResponse> getMyGamification(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gamificationService.getGamificationProfile(user.getId()));
    }

    @GetMapping("/{id}/gamification")
    public ResponseEntity<GamificationResponse> getUserGamification(@PathVariable UUID id) {
        return ResponseEntity.ok(gamificationService.getGamificationProfile(id));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<PreferencesResponse> getPreferences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferenceService.getPreferences(user.getId()));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<PreferencesResponse> updatePreferences(@AuthenticationPrincipal User user,
                                                                  @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(user, request));
    }
}
