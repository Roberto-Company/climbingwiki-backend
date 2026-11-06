package com.roberto.climbingwiki.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.model.entities.Zone;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Runs the moderation-queue queries against a real (H2, MySQL mode) database. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:unverified;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UnverifiedQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CragRepository cragRepository;

    @Autowired
    private PitchRepository pitchRepository;

    private User climber;

    @BeforeEach
    void seed() {
        climber = new User();
        climber.setUsername("climber");
        climber.setEmail("climber@example.test");
        climber.setPasswordHash("x");
        em.persist(climber);

        Zone zone = new Zone();
        zone.setName("Murcia");
        zone.setVerified(true);
        em.persist(zone);

        crag("Verified crag", zone, true);
        Crag older = crag("Older new crag", zone, false);
        crag("Newest new crag", zone, false);

        Sector sector = new Sector();
        sector.setName("Sector");
        sector.setCrag(older);
        em.persist(sector);
        Route route = new Route();
        route.setName("Marta");
        route.setSector(sector);
        route.setRouteType(Route.RouteType.SPORT);
        em.persist(route);
        Pitch pitch = new Pitch();
        pitch.setRoute(route);
        pitch.setOrderIndex(1);
        pitch.setCreatedBy(climber);
        em.persist(pitch);

        em.flush();
        em.clear();
    }

    private Crag crag(String name, Zone zone, boolean verified) {
        Crag crag = new Crag();
        crag.setName(name);
        crag.setZone(zone);
        crag.setVerified(verified);
        crag.setCreatedBy(climber);
        return em.persist(crag);
    }

    @Test
    void returnsOnlyUnverifiedItems_newestFirst_withCreatorAndParentLoaded() {
        List<Crag> crags = cragRepository.findTop100ByVerifiedFalseOrderByIdDesc();

        assertThat(crags).extracting(Crag::getName).containsExactly("Newest new crag", "Older new crag");
        assertThat(Hibernate.isInitialized(crags.get(0).getCreatedBy())).isTrue();
        assertThat(Hibernate.isInitialized(crags.get(0).getZone())).isTrue();
    }

    @Test
    void pitchesComeWithTheirRoute() {
        List<Pitch> pitches = pitchRepository.findTop100ByVerifiedFalseOrderByIdDesc();

        assertThat(pitches).hasSize(1);
        assertThat(Hibernate.isInitialized(pitches.get(0).getRoute())).isTrue();
        assertThat(pitches.get(0).getRoute().getName()).isEqualTo("Marta");
    }
}
