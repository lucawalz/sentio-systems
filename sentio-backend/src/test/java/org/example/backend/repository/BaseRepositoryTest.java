package org.example.backend.repository;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all repository integration tests.
 * Provides common test configuration and utilities.
 * 
 * @DataJpaTest - Configures an in-memory H2 database, scans for @Entity
 *              classes,
 *              and configures Spring Data JPA repositories
 *              @ActiveProfiles("test") - Activates the test profile with H2
 *              database configuration
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {

    /**
     * Helper method to print test section headers for better console output
     */
    protected void printTestHeader(String testName) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST: " + testName);
        System.out.println("=".repeat(60));
    }
}
