package com.roberto.climbingwiki.openbeta;

public record OpenBetaClimbDto(
        String uuid,
        String name,
        String fa,
        Integer length,
        OpenBetaClimbTypeDto type,
        OpenBetaGradesDto grades,
        OpenBetaClimbMetadataDto metadata) {}
