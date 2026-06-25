package com.roberto.climbingwiki.project1.openbeta;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Only active under the "openbeta-preview" profile. Fetches the same OpenBeta data the real import
 * would, but writes a review CSV instead of touching the database — run this before "openbeta-import"
 * against a shared environment.
 *
 * <p>Also caches the raw fetch to disk (see {@code openbeta.preview.cache}) so a subsequent
 * "openbeta-import" run can import from that exact reviewed payload via
 * {@code --openbeta.import.cache=<path>} instead of repeating a live API call that may hit a fresh
 * timeout.
 *
 * <p>Usage: {@code ./mvnw spring-boot:run
 * -Dspring-boot.run.profiles=openbeta-preview
 * -Dspring-boot.run.arguments="--openbeta.import.ancestors=<countryUuid>,...,<areaUuid>"}
 */
@Component
@Profile("openbeta-preview")
public class OpenBetaPreviewRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenBetaPreviewRunner.class);

    private final OpenBetaClient client;
    private final OpenBetaImportService importService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> ancestorsPath;
    private final Path outputPath;
    private final Path cachePath;

    public OpenBetaPreviewRunner(
            OpenBetaClient client,
            OpenBetaImportService importService,
            @Value("${openbeta.import.ancestors}") String ancestorsPath,
            @Value("${openbeta.preview.output:target/openbeta-preview.csv}") String outputPath,
            @Value("${openbeta.preview.cache:target/openbeta-raw-cache.json}") String cachePath) {
        this.client = client;
        this.importService = importService;
        this.ancestorsPath = List.of(ancestorsPath.split(","));
        this.outputPath = Path.of(outputPath);
        this.cachePath = Path.of(cachePath);
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Fetching OpenBeta preview for ancestors path '{}'", ancestorsPath);
        List<OpenBetaAreaDto> areas = client.fetchBulkAreas(ancestorsPath);

        Path cacheAbsolute = cachePath.toAbsolutePath();
        if (cacheAbsolute.getParent() != null) {
            Files.createDirectories(cacheAbsolute.getParent());
        }
        objectMapper.writeValue(cacheAbsolute.toFile(), areas);
        log.info(
                "Cached raw fetch to {} — pass --openbeta.import.cache={} to import this exact"
                        + " payload without a second live API call",
                cacheAbsolute,
                cacheAbsolute);

        List<OpenBetaPreviewRow> rows = importService.preview(areas);

        Path absolute = outputPath.toAbsolutePath();
        if (absolute.getParent() != null) {
            Files.createDirectories(absolute.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(absolute, StandardCharsets.UTF_8)) {
            OpenBetaPreviewCsvWriter.write(rows, writer);
        }

        long flagged = rows.stream().filter(row -> !row.issues().isBlank()).count();
        log.info("Preview written to {} ({} rows, {} flagged for review)", absolute, rows.size(), flagged);
    }
}
