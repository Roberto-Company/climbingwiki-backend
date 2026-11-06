package com.roberto.climbingwiki.openbeta;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenBetaClient {

    private static final String BULK_AREAS_QUERY =
            """
            query BulkAreas($ancestors: [String!]!) {
              bulkAreas(ancestors: $ancestors) {
                uuid
                area_name
                pathTokens
                ancestors
                metadata { leaf lat lng }
                climbs {
                  uuid
                  name
                  fa
                  length
                  type { sport trad bouldering aid ice mixed }
                  grades { french yds vscale font uiaa ewbank }
                  metadata { lat lng }
                }
              }
            }
            """;

    private final RestClient restClient;

    public OpenBetaClient(@Value("${openbeta.api-url:https://api.openbeta.io}") String apiUrl) {
        this.restClient = RestClient.create(apiUrl);
    }

    /**
     * @param ancestorsPath the root-to-target UUID chain (inclusive), e.g. {@code [countryUuid, ..., areaUuid]}.
     */
    public List<OpenBetaAreaDto> fetchBulkAreas(List<String> ancestorsPath) {
        OpenBetaBulkAreasResponse response = restClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("query", BULK_AREAS_QUERY, "variables", Map.of("ancestors", ancestorsPath)))
                .retrieve()
                .body(OpenBetaBulkAreasResponse.class);
        if (response == null || response.data() == null || response.data().bulkAreas() == null) {
            return List.of();
        }
        return response.data().bulkAreas();
    }
}
