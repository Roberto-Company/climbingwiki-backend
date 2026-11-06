package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.UnverifiedItem;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    private static final AuthenticatedUser MODERATOR = new AuthenticatedUser(3L, "mod", User.Role.MODERATOR);
    private static final AuthenticatedUser USER = new AuthenticatedUser(1L, "climber", User.Role.USER);

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CragRepository cragRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private PitchRepository pitchRepository;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(zoneRepository, cragRepository, sectorRepository, routeRepository, pitchRepository);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void unverifiedItems_regularUser_isForbiddenAndReadsNothing() {
        assertThatThrownBy(() -> service.unverifiedItems(USER)).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(zoneRepository, cragRepository, sectorRepository, routeRepository, pitchRepository);
    }

    @Test
    void unverifiedItems_listsEveryKindWithParentAndCreator() {
        User climber = user(1L, "climber");

        Crag crag = new Crag();
        crag.setId(4L);
        crag.setName("El Cejo");
        crag.setCreatedBy(climber);

        Sector sector = new Sector();
        sector.setId(7L);
        sector.setName("Gruyère");
        sector.setCrag(crag);
        sector.setCreatedBy(climber);

        Route route = new Route();
        route.setId(9L);
        route.setName("Marta");
        route.setSector(sector);

        Pitch pitch = new Pitch();
        pitch.setId(11L);
        pitch.setOrderIndex(2);
        pitch.setRoute(route);
        pitch.setCreatedBy(climber);

        when(zoneRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of());
        when(cragRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of(crag));
        when(sectorRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of(sector));
        when(routeRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of(route));
        when(pitchRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of(pitch));

        List<UnverifiedItem> items = service.unverifiedItems(MODERATOR);

        assertThat(items).containsExactly(
                new UnverifiedItem("crag", 4L, "El Cejo", null, null, null, 1L, "climber"),
                new UnverifiedItem("sector", 7L, "Gruyère", null, 4L, "El Cejo", 1L, "climber"),
                new UnverifiedItem("route", 9L, "Marta", null, 7L, "Gruyère", null, null),
                new UnverifiedItem("pitch", 11L, "Marta", 2, 9L, "Marta", 1L, "climber"));
    }

    @Test
    void unverifiedItems_zoneShowsItsParentZone() {
        Zone parent = new Zone();
        parent.setId(1L);
        parent.setName("Murcia");
        Zone zone = new Zone();
        zone.setId(2L);
        zone.setName("Mula");
        zone.setParentZone(parent);
        when(zoneRepository.findTop100ByVerifiedFalseOrderByIdDesc()).thenReturn(List.of(zone));

        List<UnverifiedItem> items = service.unverifiedItems(MODERATOR);

        assertThat(items).containsExactly(new UnverifiedItem("zone", 2L, "Mula", null, 1L, "Murcia", null, null));
    }
}
