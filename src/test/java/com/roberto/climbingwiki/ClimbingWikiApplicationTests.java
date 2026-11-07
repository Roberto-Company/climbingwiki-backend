package com.roberto.climbingwiki;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-secret-at-least-32-characters-long-for-hs256")
class ClimbingWikiApplicationTests {

    @Test
    void contextLoads() {
    }

}
