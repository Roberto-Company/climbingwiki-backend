package com.roberto.climbingwiki.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.model.GradingTypes;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Runs the pitch summary query against a real (H2, MySQL mode) database. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pitchsummary;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PitchSummaryQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PitchRepository pitchRepository;

    @Test
    void findSummaryRows_returnsOneRowPerPitchOfTheRequestedRoutesOnly() {
        GradingSystem french = new GradingSystem();
        french.setName("French");
        french.setCategory(GradingTypes.Category.ROPED);
        em.persist(french);
        Grade sixA = grade(french, "6a", 60);

        Crag crag = new Crag();
        crag.setName("Cejo");
        em.persist(crag);
        Sector sector = new Sector();
        sector.setName("A");
        sector.setCrag(crag);
        em.persist(sector);

        Route requested = route("Marta", sector);
        Route other = route("Pilar", sector);
        pitch(requested, 1, sixA, 25);
        pitch(requested, 2, null, null);
        pitch(other, 1, sixA, 30);
        em.flush();

        List<PitchSummaryRow> rows = pitchRepository.findSummaryRows(List.of(requested.getId()));

        assertThat(rows).containsExactlyInAnyOrder(
                new PitchSummaryRow(requested.getId(), sixA.getId(), 60, 25),
                new PitchSummaryRow(requested.getId(), null, null, null));
    }

    private Grade grade(GradingSystem system, String label, int score) {
        Grade grade = new Grade();
        grade.setGradingSystem(system);
        grade.setLabel(label);
        grade.setDifficultyScore(score);
        return em.persist(grade);
    }

    private Route route(String name, Sector sector) {
        Route route = new Route();
        route.setName(name);
        route.setSector(sector);
        route.setRouteType(Route.RouteType.SPORT);
        return em.persist(route);
    }

    private void pitch(Route route, int order, Grade grade, Integer lengthM) {
        Pitch pitch = new Pitch();
        pitch.setRoute(route);
        pitch.setOrderIndex(order);
        pitch.setGrade(grade);
        pitch.setLengthM(lengthM);
        em.persist(pitch);
    }
}
