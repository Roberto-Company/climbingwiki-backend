package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.exceptions.AvatarNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidImageException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.model.entities.UserAvatar;
import com.roberto.climbingwiki.repositories.UserAvatarRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.time.Clock;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uploaded profile pictures. The image is stored in the database and served by the API. The user's
 * {@code avatarUrl} points at it with a version parameter, so a new picture is never hidden by a cache.
 */
@Service
public class AvatarService {

    /** Generous cap: the frontend shrinks pictures to a few dozen KB before uploading. */
    static final int MAX_BYTES = 2 * 1024 * 1024;

    private static final int[] JPEG = {0xFF, 0xD8, 0xFF};
    private static final int[] PNG = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] RIFF = {'R', 'I', 'F', 'F'};
    private static final int[] WEBP = {'W', 'E', 'B', 'P'};

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final Clock clock;

    @Autowired
    public AvatarService(UserRepository userRepository, UserAvatarRepository avatarRepository) {
        this(userRepository, avatarRepository, Clock.systemUTC());
    }

    AvatarService(UserRepository userRepository, UserAvatarRepository avatarRepository, Clock clock) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.clock = clock;
    }

    /** A stored picture, ready to send to a browser. */
    public record AvatarImage(String contentType, byte[] data) {}

    /** Only the user (or an admin) may set their picture. */
    @Transactional
    public UserResponse upload(Long userId, byte[] data, AuthenticatedUser requester) {
        if (requester == null || !requester.canSeePrivateDataOf(userId)) {
            throw new ForbiddenException();
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (data == null || data.length == 0) {
            throw new InvalidImageException("The file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidImageException("The picture must be 2 MB or smaller");
        }
        String contentType = detectContentType(data);

        UserAvatar avatar = avatarRepository.findById(userId).orElseGet(UserAvatar::new);
        avatar.setUserId(userId);
        avatar.setContentType(contentType);
        avatar.setData(data);
        avatarRepository.save(avatar);

        user.setAvatarUrl(avatarPath(userId) + "?v=" + clock.millis());
        return UserResponse.forViewer(userRepository.save(user), requester);
    }

    @Transactional(readOnly = true)
    public AvatarImage find(Long userId) {
        return avatarRepository.findById(userId)
                .map(avatar -> new AvatarImage(avatar.getContentType(), avatar.getData()))
                .orElseThrow(() -> new AvatarNotFoundException(userId));
    }

    /** The user, or a moderator or admin (to remove an inappropriate picture), may remove it. */
    @Transactional
    public UserResponse delete(Long userId, AuthenticatedUser requester) {
        if (requester == null || !requester.isSelfOrModeratorOrAdmin(userId)) {
            throw new ForbiddenException();
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (avatarRepository.existsById(userId)) {
            avatarRepository.deleteById(userId);
        }
        user.setAvatarUrl(null);
        return UserResponse.forViewer(userRepository.save(user), requester);
    }

    static String avatarPath(Long userId) {
        return "/api/users/" + userId + "/avatar";
    }

    /** Trusts the file's bytes, never its name or declared type. */
    static String detectContentType(byte[] data) {
        if (hasSignature(data, 0, JPEG)) {
            return "image/jpeg";
        }
        if (hasSignature(data, 0, PNG)) {
            return "image/png";
        }
        if (hasSignature(data, 0, RIFF) && hasSignature(data, 8, WEBP)) {
            return "image/webp";
        }
        throw new InvalidImageException("Only JPEG, PNG and WebP pictures are accepted");
    }

    private static boolean hasSignature(byte[] data, int offset, int[] signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        byte[] expected = new byte[signature.length];
        for (int i = 0; i < signature.length; i++) {
            expected[i] = (byte) signature[i];
        }
        return Arrays.equals(data, offset, offset + signature.length, expected, 0, signature.length);
    }
}
