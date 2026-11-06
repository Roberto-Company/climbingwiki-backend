package com.roberto.climbingwiki.openbeta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenBetaAreaDto(
        String uuid,
        @JsonProperty("area_name") String areaName,
        List<String> pathTokens,
        List<String> ancestors,
        OpenBetaAreaMetadataDto metadata,
        List<OpenBetaClimbDto> climbs) {

    public boolean isLeaf() {
        return metadata != null && Boolean.TRUE.equals(metadata.leaf());
    }

    public String directParentUuid() {
        if (ancestors == null || ancestors.isEmpty()) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    public int depth() {
        return pathTokens == null ? 0 : pathTokens.size();
    }
}
