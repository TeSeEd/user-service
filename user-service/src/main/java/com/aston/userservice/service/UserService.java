package com.aston.userservice.service;

import com.aston.userservice.entity.User;
import com.aston.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        return userRepository.save(new User(name, email, age));
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
        getUserById(id);
        userRepository.deleteById(id);
    }
}
