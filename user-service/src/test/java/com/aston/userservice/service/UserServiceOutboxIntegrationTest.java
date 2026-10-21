
package com.aston.userservice.service;

import com.aston.contracts.event.UserEventType;
import com.aston.userservice.entity.User;
import com.aston.userservice.outbox.OutboxEvent;
import com.aston.userservice.outbox.OutboxEventRepository;
import com.aston.userservice.outbox.OutboxStatus;
import com.aston.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class UserServiceOutboxIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("user_service_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_shouldPersistUserAndOutboxEvent() {
        User createdUser = userService.createUser(
                "Иван",
                "test@example.com",
                25
        );

        assertNotNull(createdUser.getId());
        assertTrue(userRepository.existsById(createdUser.getId()));

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertEquals(1, events.size());

        OutboxEvent event = events.get(0);

        assertEquals(UserEventType.USER_CREATED, event.getEventType());
        assertEquals(createdUser.getId(), event.getUserId());
        assertEquals(createdUser.getEmail(), event.getEmail());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());
        assertNotNull(event.getId());
    }

    @Test
    void deleteUser_shouldPersistDeletedEventAndRemoveUser() {
        User createdUser = userService.createUser(
                "Иван",
                "test@example.com",
                25
        );

        Long userId = createdUser.getId();

        userService.deleteUser(userId);

        assertFalse(userRepository.existsById(userId));

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertEquals(2, events.size());

        OutboxEvent deletedEvent = events.stream()
                .filter(event ->
                        event.getEventType() == UserEventType.USER_DELETED)
                .findFirst()
                .orElseThrow();

        assertEquals(userId, deletedEvent.getUserId());
        assertEquals("test@example.com", deletedEvent.getEmail());
        assertEquals(OutboxStatus.PENDING, deletedEvent.getStatus());
    }


    @Test
    void createUser_shouldRollbackWhenOutboxInsertFails() {
        long usersBefore = userRepository.count();
        long eventsBefore = outboxEventRepository.count();

        jdbcTemplate.execute("""
                ALTER TABLE outbox_events
                ADD CONSTRAINT chk_test_reject_created
                CHECK (event_type <> 'USER_CREATED')
                """);

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> userService.createUser(
                            "Иван",
                            "test@example.com",
                            25
                    )
            );

            assertEquals(usersBefore, userRepository.count());
            assertEquals(eventsBefore, outboxEventRepository.count());
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE outbox_events
                    DROP CONSTRAINT chk_test_reject_created
                    """);
        }
    }


    @Test
    void updateUser_shouldNotCreateAdditionalOutboxEvent() {
        User createdUser = userService.createUser(
                "Иван",
                "test@example.com",
                25
        );

        long eventsBefore = outboxEventRepository.count();

        userService.updateUser(
                createdUser.getId(),
                "Дмитрий",
                "updated@example.com",
                30
        );

        assertEquals(
                eventsBefore,
                outboxEventRepository.count()
        );

        User updatedUser = userRepository.findById(
                createdUser.getId()
        ).orElseThrow();

        assertEquals("Дмитрий", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals(30, updatedUser.getAge());
    }
}
