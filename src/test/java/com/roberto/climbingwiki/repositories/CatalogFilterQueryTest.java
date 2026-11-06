package com.roberto.climbingwiki.repositories;

import static com.roberto.climbingwiki.repositories.CatalogSpecifications.cragFilter;
import static com.roberto.climbingwiki.repositories.CatalogSpecifications.routeFilter;
import static com.roberto.climbingwiki.repositories.CatalogSpecifications.zoneFilter;
import static org.assertj.core.api.Assertions.assertThat;

import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.Zone;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Runs the catalog filter specifications against a real (H2, MySQL mode) database. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CatalogFilterQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private CragRepository cragRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    private Zone murcia;
    private Zone mula;
    private Sector sectorA;

    @BeforeEach
    void seed() {
        murcia = zone("Murcia", null);
        mula = zone("Mula", murcia);
        zone("Mulhacén", null);

        Crag cejo = crag("Cejo Cortado", mula);
        crag("Cejo Pelado", murcia);
        sectorA = sector("Sector A", cejo);
        Sector sectorB = sector("Sector B", cejo);

        route("Marta", sectorA);
        route("Martina", sectorA);
        route("Pilar", sectorA);
        route("Marta 2", sectorB);
        route("100% Mula", sectorB);
        em.flush();
    }

    private List<String> routeNames(Specification<Route> spec) {
        return routeRepository.findAll(spec, Pageable.unpaged()).map(Route::getName).getContent();
    }

    @Test
    void routes_sectorAndName_areCombined() {
        assertThat(routeNames(routeFilter(sectorA.getId(), "mart"))).containsExactlyInAnyOrder("Marta", "Martina");
    }

    @Test
    void routes_nameOnly_searchesAllSectorsCaseInsensitively() {
        assertThat(routeNames(routeFilter(null, "MARTA"))).containsExactlyInAnyOrder("Marta", "Marta 2");
    }

    @Test
    void routes_likeWildcardsInName_areMatchedLiterally() {
        assertThat(routeNames(routeFilter(null, "%"))).containsExactly("100% Mula");
        assertThat(routeNames(routeFilter(null, "_"))).isEmpty();
    }

    @Test
    void routes_noFiltersOrBlankName_returnsEverything() {
        assertThat(routeNames(routeFilter(null, null))).hasSize(5);
        assertThat(routeNames(routeFilter(null, "  "))).hasSize(5);
    }

    @Test
    void routes_sectorOnly_returnsThatSector() {
        assertThat(routeNames(routeFilter(sectorA.getId(), null)))
                .containsExactlyInAnyOrder("Marta", "Martina", "Pilar");
    }

    @Test
    void crags_zoneAndName_areCombined() {
        assertThat(cragRepository.findAll(cragFilter(mula.getId(), "cejo"), Pageable.unpaged()).map(Crag::getName))
                .containsExactly("Cejo Cortado");
    }

    @Test
    void zones_rootOnlyAndName_areCombined() {
        assertThat(zoneRepository.findAll(zoneFilter(null, true, "mu"), Pageable.unpaged()).map(Zone::getName))
                .containsExactlyInAnyOrder("Murcia", "Mulhacén");
    }

    @Test
    void zones_parentAndName_areCombined() {
        assertThat(zoneRepository.findAll(zoneFilter(murcia.getId(), false, "mula"), Pageable.unpaged()).map(Zone::getName))
                .containsExactly("Mula");
    }

    private Zone zone(String name, Zone parent) {
        Zone zone = new Zone();
        zone.setName(name);
        zone.setParentZone(parent);
        return em.persist(zone);
    }

    private Crag crag(String name, Zone zone) {
        Crag crag = new Crag();
        crag.setName(name);
        crag.setZone(zone);
        return em.persist(crag);
    }

    private Sector sector(String name, Crag crag) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setCrag(crag);
        return em.persist(sector);
    }

    private Route route(String name, Sector sector) {
        Route route = new Route();
        route.setName(name);
        route.setSector(sector);
        route.setRouteType(Route.RouteType.SPORT);
        return em.persist(route);
    }
}
