package com.aston.userservice.service;

import com.aston.contracts.event.UserEventType;
import com.aston.userservice.entity.User;
import com.aston.userservice.outbox.OutboxEvent;
import com.aston.userservice.outbox.OutboxEventRepository;
import com.aston.userservice.outbox.OutboxStatus;
import com.aston.userservice.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        User user = new User("Иван", "example@mail.ru", 25);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals(user, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.getUserById(1L)
        );

        verify(userRepository).findById(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void getUserById_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(id)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldReturnCreatedUser_whenDataIsValid() {
        User user = new User("Иван", "example@mail.ru", 25);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        User result = userService.createUser(
                user.getName(),
                user.getEmail(),
                user.getAge()
        );

        assertEquals(user, result);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertEquals(user.getName(), capturedUser.getName());
        assertEquals(user.getEmail(), capturedUser.getEmail());
        assertEquals(user.getAge(), capturedUser.getAge());

        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent capturedEvent = eventCaptor.getValue();

        assertNotNull(capturedEvent.getId());
        assertEquals(UserEventType.USER_CREATED, capturedEvent.getEventType());
        assertEquals(1L, capturedEvent.getUserId());
        assertEquals("example@mail.ru", capturedEvent.getEmail());
        assertEquals(OutboxStatus.PENDING, capturedEvent.getStatus());
        assertEquals(0, capturedEvent.getAttemptCount());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createUser_shouldThrowException_whenNameIsInvalid(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(name, "example@mail.ru", 25)
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createUser_shouldThrowException_whenEmailIsInvalid(String email) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Иван", email, 25)
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0})
    void createUser_shouldThrowException_whenAgeIsInvalid(Integer age) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Иван", "example@mail.ru", age)
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @Test
    void getAllUsers_shouldReturnUsers_whenUsersExist() {
        List<User> users = List.of(
                new User("Иван", "example@mail.ru", 25),
                new User("Дмитрий", "examplesecond@mail.ru", 30)
        );

        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(users, result);
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenUsersNotExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenUserExists() {
        User existingUser = new User("Иван", "example@mail.ru", 25);
        User newData = new User("Дмитрий", "examplesecond@mail.ru", 30);

        ReflectionTestUtils.setField(existingUser, "id", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class)))
                .thenReturn(existingUser);

        User result = userService.updateUser(
                1L,
                newData.getName(),
                newData.getEmail(),
                newData.getAge()
        );

        assertEquals(existingUser, result);
        verify(userRepository).findById(1L);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User capturedUser = captor.getValue();

        assertEquals(1L, capturedUser.getId());
        assertEquals(newData.getName(), capturedUser.getName());
        assertEquals(newData.getEmail(), capturedUser.getEmail());
        assertEquals(newData.getAge(), capturedUser.getAge());

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        User newData = new User("Дмитрий", "examplesecond@email.ru", 30);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.updateUser(
                        1L,
                        newData.getName(),
                        newData.getEmail(),
                        newData.getAge()
                )
        );

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateUser_shouldThrowException_whenNameIsInvalid(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(
                        1L,
                        name,
                        "examplesecond@mail.ru",
                        30
                )
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateUser_shouldThrowException_whenEmailIsInvalid(String email) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(
                        1L,
                        "Дмитрий",
                        email,
                        30
                )
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0})
    void updateUser_shouldThrowException_whenAgeIsInvalid(Integer age) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(
                        1L,
                        "Дмитрий",
                        "examplesecond@mail.ru",
                        age
                )
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void updateUser_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(
                        id,
                        "Дмитрий",
                        "examplesecond@mail.ru",
                        30
                )
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        User existingUser = new User("Иван", "example@mail.ru", 25);
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(existingUser);
        verify(userRepository, never()).deleteById(anyLong());

        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent capturedEvent = eventCaptor.getValue();

        assertNotNull(capturedEvent.getId());
        assertEquals(UserEventType.USER_DELETED, capturedEvent.getEventType());
        assertEquals(1L, capturedEvent.getUserId());
        assertEquals("example@mail.ru", capturedEvent.getEmail());
        assertEquals(OutboxStatus.PENDING, capturedEvent.getStatus());
        assertEquals(0, capturedEvent.getAttemptCount());
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.deleteUser(1L)
        );

        verify(userRepository).findById(1L);
        verify(userRepository, never()).delete(any(User.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, 0})
    void deleteUser_shouldThrowException_whenIdIsInvalid(Long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser(id)
        );

        verifyNoInteractions(userRepository, outboxEventRepository);
    }
}