package com.roberto.climbingwiki.openbeta;

import java.util.List;

public record OpenBetaBulkAreasResponse(Data data) {

    public record Data(List<OpenBetaAreaDto> bulkAreas) {}
}
