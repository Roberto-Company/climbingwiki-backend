-- Demo accounts for the Docker demo. Password for all three: climbing-demo
-- Local demo only: never load this file into a database reachable from the internet.

USE climbingwiki;

INSERT INTO users (id, username, email, password_hash, full_name, role, email_verified) VALUES
    (3, 'demo',      'demo@climbingwiki.local',      '$2a$10$8NvJTTKhNNI0jY69vrUFJeMDfK3JNrxCONZeaX3E.X9DXIvuLOMi2', 'Demo climber',    'USER',      TRUE),
    (4, 'moderator', 'moderator@climbingwiki.local', '$2a$10$8NvJTTKhNNI0jY69vrUFJeMDfK3JNrxCONZeaX3E.X9DXIvuLOMi2', 'Demo moderator',  'MODERATOR', TRUE),
    (5, 'admin',     'admin@climbingwiki.local',     '$2a$10$8NvJTTKhNNI0jY69vrUFJeMDfK3JNrxCONZeaX3E.X9DXIvuLOMi2', 'Demo admin',      'ADMIN',     TRUE);
