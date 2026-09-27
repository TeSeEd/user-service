package com.aston.userservice;

import com.aston.userservice.dao.UserDao;
import com.aston.userservice.dao.UserDaoImpl;
import com.aston.userservice.entity.User;
import com.aston.userservice.exception.DataAccessException;
import com.aston.userservice.service.UserService;
import com.aston.userservice.util.HibernateUtil;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        UserDao userDao = new UserDaoImpl();
        UserService userService = new UserService(userDao);
        boolean running = true;
        Scanner sc = new Scanner(System.in);

        try {
            while (running) {
                System.out.println("1 - создать пользователя");
                System.out.println("2 - найти по id");
                System.out.println("3 - показать всех");
                System.out.println("4 - обновить");
                System.out.println("5 - удалить");
                System.out.println(" ");
                System.out.print("Введите команду: ");
                int operation;
                if (sc.hasNextInt()) {
                    operation = sc.nextInt();
                    sc.nextLine();
                } else {
                    System.out.println("Некорректный ввод. Введите номер команды.");
                    sc.nextLine();
                    continue;
                }
                try {
                    switch (operation) {
                        case 1:
                            handleCreateUser(sc, userService);
                            break;
                        case 2:
                            handleFindUser(sc, userService);
                            break;
                        case 3:
                            handleShowAllUsers(sc, userService);
                            break;
                        case 4:
                            handleUpdateUser(sc, userService);
                            break;
                        case 5:
                            handleDeleteUser(sc, userService);
                            break;
                        case 0:
                            System.out.println("Выход");
                            running = false;
                            break;
                        default:
                            System.out.println("Неизвестная команда");
                    }
                } catch (DataAccessException e) {
                    System.out.println("Ошибка при работе с базой данных: " + e.getMessage());
                }
            }

        } finally {
            sc.close();
            HibernateUtil.shutdown();
        }
    }

    private static boolean handleContinue(Scanner sc) {
        int operation;
        System.out.println("Продолжить? (1-да/0-назад): ");
        if (sc.hasNextInt()) {
            operation = sc.nextInt();
            if (operation == 0) {
                sc.nextLine();
                return false;
            } else if (operation == 1) {
                sc.nextLine();
                return true;
            } else {
                System.out.println("Некорректный ввод. Введите номер команды.");
                sc.nextLine();
                return false;
            }
        } else {
            System.out.println("Некорректный ввод. Введите номер команды.");
            sc.nextLine();
            return false;
        }
    }

    private static void handleCreateUser(Scanner sc, UserService userService) {
        String name;
        String email;
        Integer age;

        System.out.println("Выбрано: 1 - Создать пользователя");
        if (!handleContinue(sc)) {
            return;
        }
        System.out.print("Введите имя пользователя: ");
        name = sc.nextLine();
        System.out.print("Введите email: ");
        email = sc.nextLine();
        System.out.print("Введите возраст: ");
        if (sc.hasNextInt()) {
            age = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("Некорректный возраст");
            sc.nextLine();
            return;
        }
        try {
            User createdUser = userService.createUser(name, email, age);
            System.out.println("Пользователь создан");
            System.out.println(createdUser);

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleFindUser(Scanner sc, UserService userService) {
        Long id;
        System.out.println("Выбрано: 2 - Найти по id");
        if (!handleContinue(sc)) {
            return;
        }

        System.out.print("Введите id: ");
        if (sc.hasNextLong()) {
            id = sc.nextLong();
            sc.nextLine();
        } else {
            System.out.println("Некорректный id");
            sc.nextLine();
            return;
        }
        try {
            User foundUser = userService.getUserById(id);
            System.out.println("Пользователь найден");
            System.out.println(foundUser);


        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleShowAllUsers(Scanner sc, UserService userService) {
        System.out.println("Выбрано: 3 - показать всех");
        if (!handleContinue(sc)) {
            return;
        }
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {

            System.out.println("Список пользователей пуст");
        } else {
            System.out.println("Список пользователей");
            users.forEach(user -> {
                System.out.println(user);
                System.out.println("=============================");
            });
        }
    }

    private static void handleUpdateUser(Scanner sc, UserService userService) {
        Long id;
        String name;
        String email;
        Integer age;
        System.out.println("Выбрано: 4 - Обновить");
        if (!handleContinue(sc)) {
            return;
        }

        System.out.print("Введите id: ");
        if (sc.hasNextLong()) {
            id = sc.nextLong();
            sc.nextLine();
        } else {
            System.out.println("Некорректный id");
            sc.nextLine();
            return;
        }
        try {
            User existingUser = userService.getUserById(id);
            System.out.println("Пользователь найден");
            System.out.println(existingUser);
            System.out.println("Если хотите оставить старое значение - нажмите Enter");
            System.out.print("Введите новое имя пользователя [" + existingUser.getName() + "]: ");
            name = sc.nextLine();
            if (name.isBlank()) {
                name = existingUser.getName();
            }
            System.out.print("Введите новый email [" + existingUser.getEmail() + "]: ");
            email = sc.nextLine();
            if (email.isBlank()) {
                email = existingUser.getEmail();
            }
            System.out.print("Введите новый возраст [" + existingUser.getAge() + "]: ");
            String ageInput = sc.nextLine();
            if (ageInput.isBlank()) {
                age = existingUser.getAge();
            } else {
                age = Integer.parseInt(ageInput);
            }
            User updatedUser = userService.updateUser(id, name, email, age);
            System.out.println("Пользователь обновлен");
            System.out.println(updatedUser);

        } catch (NumberFormatException e) {
            System.out.println("Некорректный возраст");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleDeleteUser(Scanner sc, UserService userService) {
        Long id;
        System.out.println("Выбрано: 5 - Удалить");
        if (!handleContinue(sc)) {
            return;
        }

        System.out.print("Введите id: ");
        if (sc.hasNextLong()) {
            id = sc.nextLong();
            sc.nextLine();
        } else {
            System.out.println("Некорректный id");
            sc.nextLine();
            return;
        }
        try {
            userService.deleteUser(id);
            System.out.println("Пользователь удалён");

        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.out.println(e.getMessage());
        }
    }
}
