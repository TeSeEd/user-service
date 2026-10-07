package com.aston.userservice.service;

import com.aston.userservice.dao.UserDao;
import com.aston.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        User user = new User("Иван", "example@mail.ru", 25);
        when(userDao.findById(1L))
                .thenReturn(Optional.of(user));
        User result = userService.getUserById(1L);

        assertEquals(user, result);
        verify(userDao).findById(1L);
    }

    @Test
    void getUserById_shouldThrowException_whenUserNotFound() {
        when(userDao.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.getUserById(1L));
        verify(userDao).findById(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void getUserById_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(id));

        verifyNoInteractions(userDao);
    }

    @Test
    void createUser_shouldReturnCreatedUser_whenDataIsValid() {
        User user = new User("Иван", "example@mail.ru", 25);

        when(userDao.create(any(User.class)))
                .thenReturn(user);

        User result = userService.createUser(user.getName(), user.getEmail(), user.getAge());

        assertEquals(user, result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(userDao).create(captor.capture());
        User capturedUser = captor.getValue();

        assertEquals(user.getName(), capturedUser.getName());
        assertEquals(user.getEmail(), capturedUser.getEmail());
        assertEquals(user.getAge(), capturedUser.getAge());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createUser_shouldThrowException_whenNameIsInvalid(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(name, "example@mail.ru", 25));
        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createUser_shouldThrowException_whenEmailIsInvalid(String email) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Иван", email, 25));
        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0})
    void createUser_shouldThrowException_whenAgeIsInvalid(Integer age) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Иван", "example@mail.ru", age));
        verifyNoInteractions(userDao);
    }

    @Test
    void getAllUsers_shouldReturnUsers_whenUsersExist() {
        List<User> users = List.of(
                new User("Иван", "example@mail.ru", 25),
                new User("Дмитрий", "examplesecond@mail.ru", 30)
        );

        when(userDao.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(users, result);
        verify(userDao).findAll();
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenUsersNotExist() {
        List<User> users = List.of();
        when(userDao.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userDao).findAll();
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenUserExists() {
        User existingUser = new User("Иван", "example@mail.ru", 25);
        User newData = new User("Дмитрий", "examplesecond@mail.ru", 30);

        when(userDao.findById(1L))
                .thenReturn(Optional.of(existingUser));
        when(userDao.update(any(User.class)))
                .thenReturn(existingUser);

        User result = userService.updateUser(1L, newData.getName(), newData.getEmail(), newData.getAge());

        assertEquals(existingUser, result);
        verify(userDao).findById(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).update(captor.capture());

        User capturedUser = captor.getValue();
        assertEquals(newData.getName(), capturedUser.getName());
        assertEquals(newData.getEmail(), capturedUser.getEmail());
        assertEquals(newData.getAge(), capturedUser.getAge());
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        User newData = new User("Дмитрий", "examplesecond@email.ru", 30);
        when(userDao.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.updateUser(1L, newData.getName(), newData.getEmail(), newData.getAge()));

        verify(userDao).findById(1L);
        verify(userDao, never()).update(any(User.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateUser_shouldThrowException_whenNameIsInvalid(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, name, "examplesecond@mail.ru", 30));

        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateUser_shouldThrowException_whenEmailIsInvalid(String email) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, "Дмитрий", email, 30));

        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0})
    void updateUser_shouldThrowException_whenAgeIsInvalid(Integer age) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, "Дмитрий", "examplesecond@mail.ru", age));

        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void updateUser_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(id, "Дмитрий", "examplesecond@mail.ru", 30));

        verifyNoInteractions(userDao);
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        User existingUser = new User();
        when(userDao.findById(1L))
                .thenReturn(Optional.of(existingUser));

        userService.deleteUser(1L);

        verify(userDao).findById(1L);
        verify(userDao).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        when(userDao.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.deleteUser(1L)
        );

        verify(userDao).findById(1L);
        verify(userDao, never()).deleteById(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void deleteUser_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser(id)
        );

        verifyNoInteractions(userDao);
    }
}
