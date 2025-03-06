package com.janeapp.clinicscheduling;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles( "test" )
@Testcontainers
public class BaseIntTest {

    private static final String IMAGE_VERSION = "postgres:16";

    private static final String DATABASE_NAME = "testdb";

    private static final String USERNAME = "testuser";

    private static final String PASSWORD = "testpassword";

    private static PostgreSQLContainer<?> container = new PostgreSQLContainer<>(IMAGE_VERSION)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD).withExposedPorts(5432).withReuse(true);

    @BeforeAll
    public static void beforeAll() {
        container.start();
    }

    @DynamicPropertySource
    static void overrideProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

}
