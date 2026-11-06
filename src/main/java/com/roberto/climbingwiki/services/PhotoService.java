package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.PhotoRequest;
import com.roberto.climbingwiki.dtos.PhotoResponse;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidPhotoTargetException;
import com.roberto.climbingwiki.exceptions.PhotoNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Photo;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.PhotoRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final CragRepository cragRepository;

    public PhotoService(
            PhotoRepository photoRepository,
            UserRepository userRepository,
            RouteRepository routeRepository,
            CragRepository cragRepository) {
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.cragRepository = cragRepository;
    }

    public Page<PhotoResponse> findAll(Pageable pageable) {
        return photoRepository.findAll(pageable).map(PhotoResponse::from);
    }

    public Page<PhotoResponse> findByRoute(Long routeId, Pageable pageable) {
        return photoRepository.findByRouteId(routeId, pageable).map(PhotoResponse::from);
    }

    public Page<PhotoResponse> findByCrag(Long cragId, Pageable pageable) {
        return photoRepository.findByCragId(cragId, pageable).map(PhotoResponse::from);
    }

    public PhotoResponse findById(Long id) {
        return photoRepository.findById(id)
                .map(PhotoResponse::from)
                .orElseThrow(() -> new PhotoNotFoundException(id));
    }

    @Transactional
    public PhotoResponse create(PhotoRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        Photo photo = new Photo();
        applyRequest(photo, request);
        return PhotoResponse.from(photoRepository.save(photo));
    }

    @Transactional
    public PhotoResponse update(Long id, PhotoRequest request, AuthenticatedUser requester) {
        Photo photo = photoRepository.findById(id).orElseThrow(() -> new PhotoNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(photo.getUser().getId())) {
            throw new ForbiddenException();
        }
        applyRequest(photo, request);
        return PhotoResponse.from(photoRepository.save(photo));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Photo photo = photoRepository.findById(id).orElseThrow(() -> new PhotoNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(photo.getUser().getId())) {
            throw new ForbiddenException();
        }
        photoRepository.deleteById(id);
    }

    private void applyRequest(Photo photo, PhotoRequest request) {
        if (request.routeId() == null && request.cragId() == null) {
            throw new InvalidPhotoTargetException();
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        photo.setUser(user);

        if (request.routeId() != null) {
            Route route = routeRepository.findById(request.routeId())
                    .orElseThrow(() -> new RouteNotFoundException(request.routeId()));
            photo.setRoute(route);
        } else {
            photo.setRoute(null);
        }

        if (request.cragId() != null) {
            Crag crag = cragRepository.findById(request.cragId())
                    .orElseThrow(() -> new CragNotFoundException(request.cragId()));
            photo.setCrag(crag);
        } else {
            photo.setCrag(null);
        }

        photo.setUrl(request.url());
        photo.setCaption(request.caption());
        photo.setTopo(Boolean.TRUE.equals(request.isTopo()));
    }
}
