package com.aston.userservice.dao;

import com.aston.userservice.entity.User;
import com.aston.userservice.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class UserDaoImplTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17");
    private static SessionFactory sessionFactory;
    private UserDao userDao;

    @BeforeAll
    static void beforeAll() {
        sessionFactory = HibernateUtil.buildSessionFactory(
                postgreSQLContainer.getJdbcUrl(),
                postgreSQLContainer.getUsername(),
                postgreSQLContainer.getPassword());
    }

    @BeforeEach
    void beforeEach() {
        userDao = new UserDaoImpl(sessionFactory);
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            session.createMutationQuery("delete from User").executeUpdate();

            transaction.commit();

        }
    }

    @AfterAll
    static void afterAll() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    void create_shouldReturnCreatedUser_whenUserIsValid() {
        User user = new User("Иван", "example@mail.ru", 25);

        User result = userDao.create(user);
        Optional<User> foundUser = userDao.findById(result.getId());

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
        User result = userDao.create(user);

        Optional<User> foundUser = userDao.findById(result.getId());

        assertTrue(foundUser.isPresent());
        User savedUser = foundUser.get();

        assertEquals(user.getName(), savedUser.getName());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(user.getAge(), savedUser.getAge());
    }

    @Test
    void findById_shouldReturnEmptyOptional_whenUserNotFound() {
        Optional<User> result = userDao.findById(Long.MAX_VALUE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_shouldReturnAllUsers_whenUsersExist() {
        userDao.create(new User("Иван", "example@email.ru", 25));
        userDao.create(new User("Дмитрий", "examplesecond@email.ru", 30));
        List<User> users = userDao.findAll();

        assertEquals(2, users.size());
    }

    @Test
    void findAll_shouldReturnEmptyList_whenUsersNotFound() {
        List<User> users = userDao.findAll();
        assertTrue(users.isEmpty());
    }

    @Test
    void update_shouldReturnUpdatedUser_whenUserExists() {
        User user = new User("Иван", "example@email.ru", 25);
        User savedUser = userDao.create(user);
        savedUser.setName("Дмитрий");

        User result = userDao.update(savedUser);

        Optional<User> existingUser = userDao.findById(result.getId());
        assertEquals("Дмитрий", result.getName());
        assertTrue(existingUser.isPresent());
        assertEquals("Дмитрий", existingUser.get().getName());

    }

    @Test
    void deleteById_shouldDeleteUser_whenUserExists() {
        User user = new User("Иван", "example@email.ru", 25);
        User savedUser = userDao.create(user);
        userDao.deleteById(savedUser.getId());

        Optional<User> foundUser = userDao.findById(savedUser.getId());
        assertTrue(foundUser.isEmpty());
    }
}
