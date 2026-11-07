package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.ProfileRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.dtos.UserStatsResponse;
import com.roberto.climbingwiki.exceptions.InvalidImageException;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.AvatarService;
import com.roberto.climbingwiki.services.ProfileService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Public profile of a user: editable fields, activity stats and profile picture. */
@RestController
@RequestMapping("/api/users/{id}")
public class UserProfileController {

    /** Picture URLs carry a version parameter, so a day of caching never shows a stale picture. */
    private static final CacheControl AVATAR_CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    private final ProfileService profileService;
    private final AvatarService avatarService;

    public UserProfileController(ProfileService profileService, AvatarService avatarService) {
        this.profileService = profileService;
        this.avatarService = avatarService;
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.updateProfile(id, request, principal);
    }

    @GetMapping("/stats")
    public UserStatsResponse stats(@PathVariable Long id) {
        return profileService.stats(id);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        AvatarService.AvatarImage image = avatarService.find(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(AVATAR_CACHE)
                .body(image.data());
    }

    @PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return avatarService.upload(id, readBytes(file), principal);
    }

    @DeleteMapping("/avatar")
    public UserResponse deleteAvatar(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return avatarService.delete(id, principal);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("The file could not be read");
        }
    }
}
