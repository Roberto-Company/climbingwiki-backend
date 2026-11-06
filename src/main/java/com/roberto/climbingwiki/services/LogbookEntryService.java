package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.LogbookEntryRequest;
import com.roberto.climbingwiki.dtos.LogbookEntryResponse;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.LogbookEntryNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.LogbookEntry;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.LogbookEntryRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogbookEntryService {

    private final LogbookEntryRepository logbookEntryRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public LogbookEntryService(
            LogbookEntryRepository logbookEntryRepository,
            UserRepository userRepository,
            RouteRepository routeRepository) {
        this.logbookEntryRepository = logbookEntryRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public Page<LogbookEntryResponse> findAll(Pageable pageable) {
        return logbookEntryRepository.findAll(pageable).map(LogbookEntryResponse::from);
    }

    public Page<LogbookEntryResponse> findByUser(Long userId, Pageable pageable) {
        return logbookEntryRepository.findByUserId(userId, pageable).map(LogbookEntryResponse::from);
    }

    public Page<LogbookEntryResponse> findByRoute(Long routeId, Pageable pageable) {
        return logbookEntryRepository.findByRouteId(routeId, pageable).map(LogbookEntryResponse::from);
    }

    public LogbookEntryResponse findById(Long id) {
        return logbookEntryRepository
                .findById(id)
                .map(LogbookEntryResponse::from)
                .orElseThrow(() -> new LogbookEntryNotFoundException(id));
    }

    @Transactional
    public LogbookEntryResponse create(LogbookEntryRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        LogbookEntry logbookEntry = new LogbookEntry();
        applyRequest(logbookEntry, request);
        return LogbookEntryResponse.from(logbookEntryRepository.save(logbookEntry));
    }

    @Transactional
    public LogbookEntryResponse update(Long id, LogbookEntryRequest request, AuthenticatedUser requester) {
        LogbookEntry logbookEntry = logbookEntryRepository
                .findById(id)
                .orElseThrow(() -> new LogbookEntryNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(logbookEntry.getUser().getId())) {
            throw new ForbiddenException();
        }
        applyRequest(logbookEntry, request);
        return LogbookEntryResponse.from(logbookEntryRepository.save(logbookEntry));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        LogbookEntry logbookEntry = logbookEntryRepository
                .findById(id)
                .orElseThrow(() -> new LogbookEntryNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(logbookEntry.getUser().getId())) {
            throw new ForbiddenException();
        }
        logbookEntryRepository.deleteById(id);
    }

    private void applyRequest(LogbookEntry logbookEntry, LogbookEntryRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        logbookEntry.setUser(user);

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException(request.routeId()));
        logbookEntry.setRoute(route);

        logbookEntry.setAttemptDate(request.attemptDate());
        logbookEntry.setResult(request.result());
        logbookEntry.setNotes(request.notes());
    }
}
