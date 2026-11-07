package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.ProfileRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.dtos.UserStatsResponse;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.LogbookEntry;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.FavoriteRepository;
import com.roberto.climbingwiki.repositories.FollowRepository;
import com.roberto.climbingwiki.repositories.LogbookEntryRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's public profile: the fields they edit themselves and their activity counts. */
@Service
public class ProfileService {

    /** Logbook results that count as a send. */
    static final Set<LogbookEntry.Result> SEND_RESULTS =
            EnumSet.of(LogbookEntry.Result.REDPOINT, LogbookEntry.Result.FLASH, LogbookEntry.Result.ONSIGHT);

    private final UserRepository userRepository;
    private final LogbookEntryRepository logbookEntryRepository;
    private final FavoriteRepository favoriteRepository;
    private final FollowRepository followRepository;

    public ProfileService(
            UserRepository userRepository,
            LogbookEntryRepository logbookEntryRepository,
            FavoriteRepository favoriteRepository,
            FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.logbookEntryRepository = logbookEntryRepository;
        this.favoriteRepository = favoriteRepository;
        this.followRepository = followRepository;
    }

    /** The user, or a moderator or admin (to clean up an abusive profile), may edit it. */
    @Transactional
    public UserResponse updateProfile(Long userId, ProfileRequest request, AuthenticatedUser requester) {
        if (requester == null || !requester.isSelfOrModeratorOrAdmin(userId)) {
            throw new ForbiddenException();
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.setFullName(blankToNull(request.fullName()));
        user.setBio(blankToNull(request.bio()));
        return UserResponse.forViewer(userRepository.save(user), requester);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse stats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        Long hardestSend = logbookEntryRepository
                .findGradeIdsByDifficulty(userId, SEND_RESULTS, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        return new UserStatsResponse(
                logbookEntryRepository.countByUserId(userId),
                logbookEntryRepository.countByUserIdAndResultIn(userId, SEND_RESULTS),
                hardestSend,
                favoriteRepository.countByUserId(userId),
                followRepository.countByFollowedId(userId),
                followRepository.countByFollowerId(userId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
