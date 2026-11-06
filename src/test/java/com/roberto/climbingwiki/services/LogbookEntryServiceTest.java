package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LogbookEntryServiceTest {

    @Mock
    private LogbookEntryRepository logbookEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteRepository routeRepository;

    private LogbookEntryService logbookEntryService;

    @BeforeEach
    void setUp() {
        logbookEntryService = new LogbookEntryService(logbookEntryRepository, userRepository, routeRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        return route;
    }

    private LogbookEntry logbookEntry(Long id, User user, Route route, LogbookEntry.Result result) {
        LogbookEntry logbookEntry = new LogbookEntry();
        logbookEntry.setId(id);
        logbookEntry.setUser(user);
        logbookEntry.setRoute(route);
        logbookEntry.setAttemptDate(LocalDate.of(2026, 1, 1));
        logbookEntry.setResult(result);
        return logbookEntry;
    }

    private LogbookEntryRequest request(Long userId, Long routeId, LogbookEntry.Result result) {
        return new LogbookEntryRequest(userId, routeId, LocalDate.of(2026, 1, 1), result, "notes");
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LogbookEntry> entryPage = new PageImpl<>(
                List.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.REDPOINT)), pageable, 1);
        when(logbookEntryRepository.findAll(pageable)).thenReturn(entryPage);

        Page<LogbookEntryResponse> result = logbookEntryService.findAll(pageable);

        assertThat(result.getContent())
                .extracting(LogbookEntryResponse::result)
                .containsExactly(LogbookEntry.Result.REDPOINT);
    }

    @Test
    void findByUser_returnsEntriesForThatUser() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LogbookEntry> entryPage = new PageImpl<>(
                List.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.FLASH)), pageable, 1);
        when(logbookEntryRepository.findByUserId(1L, pageable)).thenReturn(entryPage);

        Page<LogbookEntryResponse> result = logbookEntryService.findByUser(1L, pageable);

        assertThat(result.getContent()).extracting(LogbookEntryResponse::userId).containsExactly(1L);
    }

    @Test
    void findByRoute_returnsEntriesForThatRoute() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LogbookEntry> entryPage = new PageImpl<>(
                List.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ONSIGHT)), pageable, 1);
        when(logbookEntryRepository.findByRouteId(1L, pageable)).thenReturn(entryPage);

        Page<LogbookEntryResponse> result = logbookEntryService.findByRoute(1L, pageable);

        assertThat(result.getContent()).extracting(LogbookEntryResponse::routeId).containsExactly(1L);
    }

    @Test
    void findById_existingEntry_returnsResponse() {
        when(logbookEntryRepository.findById(1L))
                .thenReturn(Optional.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT)));

        LogbookEntryResponse result = logbookEntryService.findById(1L);

        assertThat(result.result()).isEqualTo(LogbookEntry.Result.ATTEMPT);
    }

    @Test
    void findById_missingEntry_throws() {
        when(logbookEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logbookEntryService.findById(99L))
                .isInstanceOf(LogbookEntryNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_missingUser_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> logbookEntryService.create(request(1L, 1L, LogbookEntry.Result.ATTEMPT), self(1L)))
                .isInstanceOf(UserNotFoundException.class);

        verify(logbookEntryRepository, never()).save(any());
    }

    @Test
    void create_missingRoute_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> logbookEntryService.create(request(1L, 1L, LogbookEntry.Result.ATTEMPT), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void create_validRequest_savesEntry() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(logbookEntryRepository.save(any(LogbookEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LogbookEntryResponse result =
                logbookEntryService.create(request(1L, 1L, LogbookEntry.Result.REDPOINT), self(1L));

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.result()).isEqualTo(LogbookEntry.Result.REDPOINT);
    }

    @Test
    void create_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> logbookEntryService.create(request(1L, 1L, LogbookEntry.Result.ATTEMPT), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(logbookEntryRepository, never()).save(any());
    }

    @Test
    void update_existingEntry_appliesChanges() {
        LogbookEntry existing = logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT);
        when(logbookEntryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(logbookEntryRepository.save(any(LogbookEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LogbookEntryResponse result =
                logbookEntryService.update(1L, request(1L, 1L, LogbookEntry.Result.ONSIGHT), self(1L));

        assertThat(result.result()).isEqualTo(LogbookEntry.Result.ONSIGHT);
    }

    @Test
    void update_missingEntry_throws() {
        when(logbookEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> logbookEntryService.update(99L, request(1L, 1L, LogbookEntry.Result.ATTEMPT), self(1L)))
                .isInstanceOf(LogbookEntryNotFoundException.class);
    }

    @Test
    void update_byAnotherRegularUser_throwsForbidden() {
        LogbookEntry existing = logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT);
        when(logbookEntryRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () -> logbookEntryService.update(1L, request(1L, 1L, LogbookEntry.Result.ONSIGHT), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(logbookEntryRepository, never()).save(any());
    }

    @Test
    void update_byModerator_succeeds() {
        LogbookEntry existing = logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT);
        when(logbookEntryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(logbookEntryRepository.save(any(LogbookEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LogbookEntryResponse result = logbookEntryService.update(
                1L, request(1L, 1L, LogbookEntry.Result.ONSIGHT), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.result()).isEqualTo(LogbookEntry.Result.ONSIGHT);
    }

    @Test
    void delete_existingEntry_deletes() {
        when(logbookEntryRepository.findById(1L))
                .thenReturn(Optional.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT)));

        logbookEntryService.delete(1L, self(1L));

        verify(logbookEntryRepository).deleteById(1L);
    }

    @Test
    void delete_missingEntry_throwsAndDoesNotDelete() {
        when(logbookEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logbookEntryService.delete(99L, self(99L)))
                .isInstanceOf(LogbookEntryNotFoundException.class);

        verify(logbookEntryRepository, never()).deleteById(99L);
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbiddenAndDoesNotDelete() {
        when(logbookEntryRepository.findById(1L))
                .thenReturn(Optional.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT)));

        assertThatThrownBy(() -> logbookEntryService.delete(1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(logbookEntryRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersEntry() {
        when(logbookEntryRepository.findById(1L))
                .thenReturn(Optional.of(logbookEntry(1L, user(1L), route(1L), LogbookEntry.Result.ATTEMPT)));

        logbookEntryService.delete(1L, admin(99L));

        verify(logbookEntryRepository).deleteById(1L);
    }
}
