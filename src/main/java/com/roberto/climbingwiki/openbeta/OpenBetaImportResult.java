package com.roberto.climbingwiki.openbeta;

public record OpenBetaImportResult(
        int zonesCreated,
        int zonesUpdated,
        int cragsCreated,
        int cragsUpdated,
        int sectorsCreated,
        int sectorsUpdated,
        int routesCreated,
        int routesUpdated,
        int gradesUnmatched) {

    static final class Counters {
        int zonesCreated;
        int zonesUpdated;
        int cragsCreated;
        int cragsUpdated;
        int sectorsCreated;
        int sectorsUpdated;
        int routesCreated;
        int routesUpdated;
        int gradesUnmatched;

        OpenBetaImportResult toResult() {
            return new OpenBetaImportResult(
                    zonesCreated,
                    zonesUpdated,
                    cragsCreated,
                    cragsUpdated,
                    sectorsCreated,
                    sectorsUpdated,
                    routesCreated,
                    routesUpdated,
                    gradesUnmatched);
        }
    }
}
