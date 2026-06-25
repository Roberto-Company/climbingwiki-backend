package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.PhotoRequest;
import com.roberto.climbingwiki.project1.dtos.PhotoResponse;
import com.roberto.climbingwiki.project1.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.InvalidPhotoTargetException;
import com.roberto.climbingwiki.project1.exceptions.PhotoNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.Photo;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.CragRepository;
import com.roberto.climbingwiki.project1.repositories.PhotoRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
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
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CragRepository cragRepository;

    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        photoService = new PhotoService(photoRepository, userRepository, routeRepository, cragRepository);
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

    private Crag crag(Long id) {
        Crag crag = new Crag();
        crag.setId(id);
        return crag;
    }

    private Photo photo(Long id, User user, Route route, Crag crag, String url) {
        Photo photo = new Photo();
        photo.setId(id);
        photo.setUser(user);
        photo.setRoute(route);
        photo.setCrag(crag);
        photo.setUrl(url);
        return photo;
    }

    private PhotoRequest request(Long userId, Long routeId, Long cragId, String url) {
        return new PhotoRequest(userId, routeId, cragId, url, "caption", false);
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
        Page<Photo> photoPage =
                new PageImpl<>(List.of(photo(1L, user(1L), route(1L), null, "a.jpg")), pageable, 1);
        when(photoRepository.findAll(pageable)).thenReturn(photoPage);

        Page<PhotoResponse> result = photoService.findAll(pageable);

        assertThat(result.getContent()).extracting(PhotoResponse::url).containsExactly("a.jpg");
    }

    @Test
    void findByRoute_returnsPhotosForThatRoute() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> photoPage =
                new PageImpl<>(List.of(photo(1L, user(1L), route(1L), null, "a.jpg")), pageable, 1);
        when(photoRepository.findByRouteId(1L, pageable)).thenReturn(photoPage);

        Page<PhotoResponse> result = photoService.findByRoute(1L, pageable);

        assertThat(result.getContent()).extracting(PhotoResponse::routeId).containsExactly(1L);
    }

    @Test
    void findByCrag_returnsPhotosForThatCrag() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> photoPage =
                new PageImpl<>(List.of(photo(1L, user(1L), null, crag(1L), "a.jpg")), pageable, 1);
        when(photoRepository.findByCragId(1L, pageable)).thenReturn(photoPage);

        Page<PhotoResponse> result = photoService.findByCrag(1L, pageable);

        assertThat(result.getContent()).extracting(PhotoResponse::cragId).containsExactly(1L);
    }

    @Test
    void findById_existingPhoto_returnsResponse() {
        when(photoRepository.findById(1L))
                .thenReturn(Optional.of(photo(1L, user(1L), route(1L), null, "a.jpg")));

        PhotoResponse result = photoService.findById(1L);

        assertThat(result.url()).isEqualTo("a.jpg");
    }

    @Test
    void findById_missingPhoto_throws() {
        when(photoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.findById(99L))
                .isInstanceOf(PhotoNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_neitherRouteNorCrag_throws() {
        assertThatThrownBy(() -> photoService.create(request(1L, null, null, "a.jpg"), self(1L)))
                .isInstanceOf(InvalidPhotoTargetException.class);

        verify(photoRepository, never()).save(any());
    }

    @Test
    void create_missingUser_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.create(request(1L, 1L, null, "a.jpg"), self(1L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_missingRoute_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.create(request(1L, 1L, null, "a.jpg"), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void create_missingCrag_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cragRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.create(request(1L, null, 1L, "a.jpg"), self(1L)))
                .isInstanceOf(CragNotFoundException.class);
    }

    @Test
    void create_withOnlyRoute_savesPhoto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoResponse result = photoService.create(request(1L, 1L, null, "a.jpg"), self(1L));

        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.cragId()).isNull();
    }

    @Test
    void create_withOnlyCrag_savesPhoto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cragRepository.findById(1L)).thenReturn(Optional.of(crag(1L)));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoResponse result = photoService.create(request(1L, null, 1L, "a.jpg"), self(1L));

        assertThat(result.cragId()).isEqualTo(1L);
        assertThat(result.routeId()).isNull();
    }

    @Test
    void create_withBothRouteAndCrag_savesPhoto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(cragRepository.findById(1L)).thenReturn(Optional.of(crag(1L)));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoResponse result = photoService.create(request(1L, 1L, 1L, "a.jpg"), self(1L));

        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.cragId()).isEqualTo(1L);
    }

    @Test
    void create_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> photoService.create(request(1L, 1L, null, "a.jpg"), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(photoRepository, never()).save(any());
    }

    @Test
    void update_existingPhoto_appliesChanges() {
        Photo existing = photo(1L, user(1L), route(1L), null, "old.jpg");
        when(photoRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoResponse result = photoService.update(1L, request(1L, 1L, null, "new.jpg"), self(1L));

        assertThat(result.url()).isEqualTo("new.jpg");
    }

    @Test
    void update_missingPhoto_throws() {
        when(photoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.update(99L, request(1L, 1L, null, "a.jpg"), self(1L)))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    void update_byAnotherRegularUser_throwsForbidden() {
        Photo existing = photo(1L, user(1L), route(1L), null, "old.jpg");
        when(photoRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> photoService.update(1L, request(1L, 1L, null, "new.jpg"), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(photoRepository, never()).save(any());
    }

    @Test
    void update_byModerator_succeeds() {
        Photo existing = photo(1L, user(1L), route(1L), null, "old.jpg");
        when(photoRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoResponse result = photoService.update(
                1L, request(1L, 1L, null, "new.jpg"), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.url()).isEqualTo("new.jpg");
    }

    @Test
    void delete_existingPhoto_deletes() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo(1L, user(1L), route(1L), null, "a.jpg")));

        photoService.delete(1L, self(1L));

        verify(photoRepository).deleteById(1L);
    }

    @Test
    void delete_missingPhoto_throwsAndDoesNotDelete() {
        when(photoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.delete(99L, self(99L))).isInstanceOf(PhotoNotFoundException.class);

        verify(photoRepository, never()).deleteById(99L);
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbiddenAndDoesNotDelete() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo(1L, user(1L), route(1L), null, "a.jpg")));

        assertThatThrownBy(() -> photoService.delete(1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(photoRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersPhoto() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo(1L, user(1L), route(1L), null, "a.jpg")));

        photoService.delete(1L, admin(99L));

        verify(photoRepository).deleteById(1L);
    }
}
