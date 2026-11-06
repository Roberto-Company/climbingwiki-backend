package com.roberto.climbingwiki.seed;

import com.roberto.climbingwiki.model.GradingTypes;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.repositories.GradeRepository;
import com.roberto.climbingwiki.repositories.GradingSystemRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds grading_systems/grades from a CSV ({@code grading_system,category,label,difficulty_score}).
 * Idempotent: existing rows (matched by name / label) are left untouched, so re-running after an
 * admin has hand-tuned a difficulty_score via the Grade CRUD endpoints won't overwrite it.
 */
@Service
public class GradeSeedService {

    private final GradingSystemRepository gradingSystemRepository;
    private final GradeRepository gradeRepository;

    public GradeSeedService(GradingSystemRepository gradingSystemRepository, GradeRepository gradeRepository) {
        this.gradingSystemRepository = gradingSystemRepository;
        this.gradeRepository = gradeRepository;
    }

    @Transactional
    public GradeSeedResult seed(Reader csvReader) {
        int systemsCreated = 0;
        int systemsExisting = 0;
        int gradesCreated = 0;
        int gradesExisting = 0;
        Map<String, GradingSystem> systemsByName = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(csvReader)) {
            String header = reader.readLine();
            if (header == null) {
                return new GradeSeedResult(0, 0, 0, 0);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                String systemName = columns[0].trim();
                GradingTypes.Category category = GradingTypes.Category.valueOf(columns[1].trim());
                String label = columns[2].trim();
                int difficultyScore = Integer.parseInt(columns[3].trim());

                GradingSystem system = systemsByName.get(systemName);
                if (system == null) {
                    GradingSystem existing = gradingSystemRepository.findByName(systemName).orElse(null);
                    if (existing == null) {
                        GradingSystem created = new GradingSystem();
                        created.setName(systemName);
                        created.setCategory(category);
                        system = gradingSystemRepository.save(created);
                        systemsCreated++;
                    } else {
                        system = existing;
                        systemsExisting++;
                    }
                    systemsByName.put(systemName, system);
                }

                boolean gradeExists =
                        gradeRepository.findByGradingSystemIdAndLabel(system.getId(), label).isPresent();
                if (gradeExists) {
                    gradesExisting++;
                    continue;
                }
                Grade grade = new Grade();
                grade.setGradingSystem(system);
                grade.setLabel(label);
                grade.setDifficultyScore(difficultyScore);
                gradeRepository.save(grade);
                gradesCreated++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read grade seed CSV", e);
        }

        return new GradeSeedResult(systemsCreated, systemsExisting, gradesCreated, gradesExisting);
    }
}
