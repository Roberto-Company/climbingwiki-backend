package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.FollowRequest;
import com.roberto.climbingwiki.dtos.FollowResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.FollowNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidFollowException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.Follow;
import com.roberto.climbingwiki.model.entities.FollowId;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.FollowRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    public List<FollowResponse> findAll() {
        return followRepository.findAll().stream().map(FollowResponse::from).toList();
    }

    public List<FollowResponse> findByFollower(Long followerId) {
        return followRepository.findByFollowerId(followerId).stream().map(FollowResponse::from).toList();
    }

    public List<FollowResponse> findByFollowed(Long followedId) {
        return followRepository.findByFollowedId(followedId).stream().map(FollowResponse::from).toList();
    }

    public FollowResponse findOne(Long followerId, Long followedId) {
        return followRepository
                .findById(new FollowId(followerId, followedId))
                .map(FollowResponse::from)
                .orElseThrow(() -> new FollowNotFoundException(followerId, followedId));
    }

    @Transactional
    public UpsertResult<FollowResponse> upsert(FollowRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.followerId())) {
            throw new ForbiddenException();
        }
        if (request.followerId().equals(request.followedId())) {
            throw new InvalidFollowException();
        }

        FollowId id = new FollowId(request.followerId(), request.followedId());
        boolean created = !followRepository.existsById(id);

        Follow follow = followRepository.findById(id).orElseGet(Follow::new);

        User follower = userRepository
                .findById(request.followerId())
                .orElseThrow(() -> new UserNotFoundException(request.followerId()));
        User followed = userRepository
                .findById(request.followedId())
                .orElseThrow(() -> new UserNotFoundException(request.followedId()));

        follow.setFollower(follower);
        follow.setFollowed(followed);

        Follow saved = followRepository.save(follow);
        return new UpsertResult<>(FollowResponse.from(saved), created);
    }

    @Transactional
    public void delete(Long followerId, Long followedId, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(followerId)) {
            throw new ForbiddenException();
        }
        FollowId id = new FollowId(followerId, followedId);
        if (followRepository.existsById(id)) {
            followRepository.deleteById(id);
        }
    }
}
