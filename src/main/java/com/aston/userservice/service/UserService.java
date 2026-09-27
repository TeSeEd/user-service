package com.aston.userservice.service;

import com.aston.userservice.dao.UserDao;
import com.aston.userservice.entity.User;

import java.util.List;
import java.util.NoSuchElementException;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

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

        return userDao.create(new User(name, email, age));
    }

    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный id");
        }
        return userDao.findById(id).orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не найден"));
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

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
        return userDao.update(existingUser);
    }

    public void deleteUser(Long id) {
        getUserById(id);
        userDao.deleteById(id);
    }
}
