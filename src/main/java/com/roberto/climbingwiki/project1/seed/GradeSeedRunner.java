package com.roberto.climbingwiki.project1.seed;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Only active under the "seed-grades" profile, so a normal app boot never re-seeds.
 *
 * <p>Usage: {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-grades}
 */
@Component
@Profile("seed-grades")
public class GradeSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GradeSeedRunner.class);

    private final GradeSeedService seedService;

    public GradeSeedRunner(GradeSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(String... args) throws IOException {
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("seed/grades.csv").getInputStream(), StandardCharsets.UTF_8)) {
            GradeSeedResult result = seedService.seed(reader);
            log.info(
                    "Grade seed finished: systems[+{} ~{}] grades[+{} ~{}]",
                    result.systemsCreated(),
                    result.systemsExisting(),
                    result.gradesCreated(),
                    result.gradesExisting());
        }
    }
}
