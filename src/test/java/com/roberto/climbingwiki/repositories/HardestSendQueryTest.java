package com.roberto.climbingwiki.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.roberto.climbingwiki.model.GradingTypes;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.model.entities.LogbookEntry;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.User;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

/** Runs the profile stats queries against a real (H2, MySQL mode) database. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:hardestsend;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HardestSendQueryTest {

    private static final Set<LogbookEntry.Result> SENDS =
            EnumSet.of(LogbookEntry.Result.REDPOINT, LogbookEntry.Result.FLASH, LogbookEntry.Result.ONSIGHT);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LogbookEntryRepository logbookEntryRepository;

    private Sector sector;
    private Grade sixA;
    private Grade sevenA;
    private Grade eightA;

    @BeforeEach
    void setUp() {
        GradingSystem french = new GradingSystem();
        french.setName("French");
        french.setCategory(GradingTypes.Category.ROPED);
        em.persist(french);
        sixA = grade(french, "6a", 60);
        sevenA = grade(french, "7a", 70);
        eightA = grade(french, "8a", 80);

        Crag crag = new Crag();
        crag.setName("Mula");
        em.persist(crag);
        sector = new Sector();
        sector.setName("Gruyer");
        sector.setCrag(crag);
        em.persist(sector);
    }

    @Test
    void hardestSend_isTheHardestPitchOfASentRoute_ignoringAttemptsAndOtherUsers() {
        User climber = user("climber");
        User other = user("other");
        Route multiPitch = route("Two pitches", sixA, sevenA);
        Route project = route("Project", eightA);
        log(climber, multiPitch, LogbookEntry.Result.REDPOINT);
        log(climber, project, LogbookEntry.Result.ATTEMPT);
        log(other, project, LogbookEntry.Result.ONSIGHT);
        em.flush();

        var ids = logbookEntryRepository.findGradeIdsByDifficulty(climber.getId(), SENDS, PageRequest.of(0, 1));

        assertThat(ids).containsExactly(sevenA.getId());
        assertThat(logbookEntryRepository.countByUserId(climber.getId())).isEqualTo(2);
        assertThat(logbookEntryRepository.countByUserIdAndResultIn(climber.getId(), SENDS)).isEqualTo(1);
    }

    @Test
    void hardestSend_withOnlyAttempts_isEmpty() {
        User climber = user("climber");
        log(climber, route("Project", eightA), LogbookEntry.Result.ATTEMPT);
        em.flush();

        assertThat(logbookEntryRepository.findGradeIdsByDifficulty(climber.getId(), SENDS, PageRequest.of(0, 1)))
                .isEmpty();
    }

    private Grade grade(GradingSystem system, String label, int score) {
        Grade grade = new Grade();
        grade.setGradingSystem(system);
        grade.setLabel(label);
        grade.setDifficultyScore(score);
        return em.persist(grade);
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("!");
        return em.persist(user);
    }

    private Route route(String name, Grade... pitchGrades) {
        Route route = new Route();
        route.setName(name);
        route.setSector(sector);
        route.setRouteType(Route.RouteType.SPORT);
        em.persist(route);
        for (int i = 0; i < pitchGrades.length; i++) {
            Pitch pitch = new Pitch();
            pitch.setRoute(route);
            pitch.setOrderIndex(i + 1);
            pitch.setGrade(pitchGrades[i]);
            em.persist(pitch);
        }
        return route;
    }

    private void log(User user, Route route, LogbookEntry.Result result) {
        LogbookEntry entry = new LogbookEntry();
        entry.setUser(user);
        entry.setRoute(route);
        entry.setAttemptDate(LocalDate.of(2026, 5, 1));
        entry.setResult(result);
        em.persist(entry);
    }
}
