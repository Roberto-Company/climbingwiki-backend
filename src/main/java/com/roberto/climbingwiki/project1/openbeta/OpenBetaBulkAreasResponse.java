package com.roberto.climbingwiki.project1.openbeta;

import java.util.List;

public record OpenBetaBulkAreasResponse(Data data) {

    public record Data(List<OpenBetaAreaDto> bulkAreas) {}
}
