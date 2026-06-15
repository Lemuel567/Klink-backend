package com.example.demo;
//Integration test -verifies spring context loads correctly  with live PostgreSQL

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test — requires live PostgreSQL on localhost:54321. Run with: .\\mvnw.cmd test -Dtest=Demo5ApplicationTests")
class Demo5ApplicationTests {

    @Test
    void contextLoads() {
    }

}
