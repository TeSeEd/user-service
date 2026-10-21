package com.aston.userservice.service;

import com.aston.contracts.event.UserEventType;
import com.aston.userservice.entity.User;
import com.aston.userservice.outbox.OutboxEvent;
import com.aston.userservice.outbox.OutboxEventRepository;
import com.aston.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final OutboxEventRepository outboxEventRepository;

    public UserService(UserRepository userRepository, OutboxEventRepository outboxEventRepository) {

        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public User createUser(String name, String email, Integer age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Некорректное имя пользователя");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Некорректный email");
        }

        if (age == null || age <= 0) {
            throw new IllegalArgumentException("Некорректный возраст");
        }
        User savedUser = userRepository.save(new User(name, email, age));

        OutboxEvent event = new OutboxEvent(
                UserEventType.USER_CREATED,
                savedUser.getId(),
                savedUser.getEmail()
        );
        outboxEventRepository.save(event);
        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный id");
        }
        return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, String name, String email, Integer age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Некорректное имя пользователя");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Некорректный email");
        }
        if (age == null || age <= 0) {
            throw new IllegalArgumentException("Некорректный возраст");
        }
        User existingUser = getUserById(id);
        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setAge(age);
        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        OutboxEvent event = new OutboxEvent(
                UserEventType.USER_DELETED,
                user.getId(),
                user.getEmail()
        );

        userRepository.delete(user);
        outboxEventRepository.save(event);
    }
}
