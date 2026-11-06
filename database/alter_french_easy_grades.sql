-- French grades: use the Spanish-topo convention for the easy end of the scale,
-- 3, 4, 4+, 5, 5+, 6a, 6a+... instead of theCrag's 5a / 5b / 5c.
--   before: ... 5=4, 5a=5, 5b=6, 5c=7, 6a=8 ...
--   after:  ... 5=4, 5+=6, 6a=8 ...  (scores only order grades within a system; gaps are fine)
-- Existing pitches: 5a and 5b become 5, 5c becomes 5+. Then 5a/5b/5c are removed.
-- Run with: mysql --default-character-set=utf8mb4 ... < database/alter_french_easy_grades.sql

USE climbingwiki;

START TRANSACTION;

SET @french = (SELECT id FROM grading_systems WHERE name = 'French');

INSERT INTO grades (grading_system_id, label, difficulty_score)
SELECT @french, '5+', 6
WHERE NOT EXISTS (SELECT 1 FROM grades WHERE grading_system_id = @french AND label = '5+');

SET @five = (SELECT id FROM grades WHERE grading_system_id = @french AND label = '5');
SET @five_plus = (SELECT id FROM grades WHERE grading_system_id = @french AND label = '5+');

UPDATE pitches p
JOIN grades g ON g.id = p.grade_id
SET p.grade_id = CASE g.label WHEN '5c' THEN @five_plus ELSE @five END
WHERE g.grading_system_id = @french AND g.label IN ('5a', '5b', '5c');

DELETE FROM grades WHERE grading_system_id = @french AND label IN ('5a', '5b', '5c');

COMMIT;
