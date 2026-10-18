package com.aston.userservice.repository;

import com.aston.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17");
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void save_shouldReturnCreatedUser_whenUserIsValid() {
        User user = new User("Иван", "example@mail.ru", 25);

        User result = userRepository.save(user);

        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findById(result.getId());

        assertNotNull(result.getId());
        assertTrue(foundUser.isPresent());

        User savedUser = foundUser.get();

        assertEquals(user.getName(), savedUser.getName());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(user.getAge(), savedUser.getAge());
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        User user = new User("Иван", "example@mail.ru", 25);
        User result = userRepository.save(user);

        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findById(result.getId());

        assertTrue(foundUser.isPresent());
        User savedUser = foundUser.get();

        assertEquals(user.getName(), savedUser.getName());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(user.getAge(), savedUser.getAge());
    }

    @Test
    void findById_shouldReturnEmptyOptional_whenUserNotFound() {
        Optional<User> result = userRepository.findById(Long.MAX_VALUE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_shouldReturnAllUsers_whenUsersExist() {
        userRepository.save(new User("Иван", "example@email.ru", 25));
        userRepository.save(new User("Дмитрий", "examplesecond@email.ru", 30));

        testEntityManager.flush();
        testEntityManager.clear();

        List<User> users = userRepository.findAll();

        assertEquals(2, users.size());

        assertTrue(users.stream()
                .anyMatch(user -> "example@email.ru".equals(user.getEmail())));

        assertTrue(users.stream()
                .anyMatch(user -> "examplesecond@email.ru".equals(user.getEmail())));
    }

    @Test
    void findAll_shouldReturnEmptyList_whenUsersNotFound() {
        List<User> users = userRepository.findAll();
        assertTrue(users.isEmpty());
    }

    @Test
    void update_shouldReturnUpdatedUser_whenUserExists() {
        User user = new User("Иван", "example@email.ru", 25);
        User savedUser = userRepository.save(user);

        testEntityManager.flush();
        testEntityManager.clear();

        User existingUser = userRepository.findById(savedUser.getId()).orElseThrow();
        existingUser.setName("Дмитрий");

        User result = userRepository.save(existingUser);

        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> updatedUser = userRepository.findById(result.getId());

        assertTrue(updatedUser.isPresent());
        assertEquals("Дмитрий", updatedUser.get().getName());

    }

    @Test
    void deleteById_shouldDeleteUser_whenUserExists() {
        User user = new User("Иван", "example@email.ru", 25);
        User savedUser = userRepository.save(user);

        testEntityManager.flush();
        testEntityManager.clear();

        userRepository.deleteById(savedUser.getId());

        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isEmpty());
    }
}




