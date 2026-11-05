package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Zone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters for catalog list endpoints. Combine them with {@link Specification#allOf}. */
public final class CatalogSpecifications {

    /** Not a backslash: MySQL treats backslashes in string literals as escapes. */
    private static final char LIKE_ESCAPE = '!';

    private CatalogSpecifications() {}

    /** {@code GET /api/routes}: every non-null filter applies (AND). No filters = all routes. */
    public static Specification<Route> routeFilter(Long sectorId, String name) {
        List<Specification<Route>> filters = new ArrayList<>();
        if (sectorId != null) {
            filters.add(parentIdEquals("sector", sectorId));
        }
        addNameFilter(filters, name);
        return Specification.allOf(filters);
    }

    /** {@code GET /api/crags}: every non-null filter applies (AND). */
    public static Specification<Crag> cragFilter(Long zoneId, String name) {
        List<Specification<Crag>> filters = new ArrayList<>();
        if (zoneId != null) {
            filters.add(parentIdEquals("zone", zoneId));
        }
        addNameFilter(filters, name);
        return Specification.allOf(filters);
    }

    /**
     * {@code GET /api/zones}: every filter applies (AND). {@code parentZoneId} together with
     * {@code rootOnly} can never match, so that combination simply returns nothing.
     */
    public static Specification<Zone> zoneFilter(Long parentZoneId, boolean rootOnly, String name) {
        List<Specification<Zone>> filters = new ArrayList<>();
        if (parentZoneId != null) {
            filters.add(parentIdEquals("parentZone", parentZoneId));
        }
        if (rootOnly) {
            filters.add(parentIsNull("parentZone"));
        }
        addNameFilter(filters, name);
        return Specification.allOf(filters);
    }

    private static <T> void addNameFilter(List<Specification<T>> filters, String name) {
        if (name != null && !name.isBlank()) {
            filters.add(nameContains(name));
        }
    }

    /** Case-insensitive "name contains"; {@code %} and {@code _} in the input match literally. */
    public static <T> Specification<T> nameContains(String name) {
        String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    /** {@code association.id = id}, e.g. {@code parentIdEquals("sector", 6L)}. */
    public static <T> Specification<T> parentIdEquals(String association, Long id) {
        return (root, query, cb) -> cb.equal(root.get(association).get("id"), id);
    }

    /** {@code association IS NULL}, e.g. top-level zones. */
    public static <T> Specification<T> parentIsNull(String association) {
        return (root, query, cb) -> cb.isNull(root.get(association));
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
