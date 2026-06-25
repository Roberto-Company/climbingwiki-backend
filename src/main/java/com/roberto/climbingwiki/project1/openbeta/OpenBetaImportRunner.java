package com.roberto.climbingwiki.project1.openbeta;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Only active under the "openbeta-import" profile, so a normal app boot never triggers an import.
 *
 * <p>Usage: {@code ./mvnw spring-boot:run
 * -Dspring-boot.run.profiles=openbeta-import
 * -Dspring-boot.run.arguments="--openbeta.import.ancestors=<countryUuid>,...,<areaUuid>"}
 *
 * <p>The ancestors path is the root-to-target UUID chain OpenBeta's {@code bulkAreas} query
 * expects (comma-separated here, split into a list before calling the API).
 *
 * <p>If {@code --openbeta.import.cache=<path>} points at a JSON file written by the
 * "openbeta-preview" run, that exact reviewed payload is imported with no live API call — so a
 * flaky OpenBeta endpoint only has to succeed once across the whole preview-then-import workflow.
 * Without it, this falls back to a fresh live fetch.
 */
@Component
@Profile("openbeta-import")
public class OpenBetaImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenBetaImportRunner.class);

    private final OpenBetaClient client;
    private final OpenBetaImportService importService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> ancestorsPath;
    private final String cachePath;

    public OpenBetaImportRunner(
            OpenBetaClient client,
            OpenBetaImportService importService,
            @Value("${openbeta.import.ancestors}") String ancestorsPath,
            @Value("${openbeta.import.cache:}") String cachePath) {
        this.client = client;
        this.importService = importService;
        this.ancestorsPath = List.of(ancestorsPath.split(","));
        this.cachePath = cachePath;
    }

    @Override
    public void run(String... args) throws IOException {
        List<OpenBetaAreaDto> areas;
        if (cachePath != null && !cachePath.isBlank()) {
            Path absolute = Path.of(cachePath).toAbsolutePath();
            log.info("Importing from cached OpenBeta fetch at {} (no live API call)", absolute);
            areas = List.of(objectMapper.readValue(absolute.toFile(), OpenBetaAreaDto[].class));
        } else {
            log.info("Starting OpenBeta import for ancestors path '{}' (live fetch)", ancestorsPath);
            areas = client.fetchBulkAreas(ancestorsPath);
        }

        OpenBetaImportResult result = importService.importAreas(areas);
        log.info(
                "OpenBeta import finished: zones[+{} ~{}] crags[+{} ~{}] sectors[+{} ~{}] routes[+{} ~{}]"
                        + " grades unmatched={}",
                result.zonesCreated(),
                result.zonesUpdated(),
                result.cragsCreated(),
                result.cragsUpdated(),
                result.sectorsCreated(),
                result.sectorsUpdated(),
                result.routesCreated(),
                result.routesUpdated(),
                result.gradesUnmatched());
    }
}
