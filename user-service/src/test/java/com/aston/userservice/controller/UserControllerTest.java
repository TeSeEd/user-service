package com.aston.userservice.controller;

import com.aston.userservice.entity.User;
import com.aston.userservice.mapper.UserMapper;
import com.aston.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(UserMapper.class)
@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createUser_shouldReturnCreated_whenRequestIsValid() throws Exception {
        User user = new User("Иван", "example@email.com", 25);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.createUser(
                user.getName(),
                user.getEmail(),
                user.getAge())
        )
                .thenReturn(user);

        String requestJson = """
                {
                "name": "Иван",
                "email": "example@email.com",
                "age": 25
                }
                """;
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("example@email.com"))
                .andExpect(jsonPath("$.age").value(25));

        verify(userService).createUser(user.getName(), user.getEmail(), user.getAge());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        String requestJson = """
                {
                  "name": "",
                  "email": "invalid-email",
                  "age": -5
                }
                """;
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("age")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() throws Exception {
        User user = new User("Иван", "example@email.com", 25);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("example@email.com"))
                .andExpect(jsonPath("$.age").value(25));

        verify(userService).getUserById(1L);
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new NoSuchElementException("Пользователь с id: 999 не найден"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).getUserById(999L);
    }

    @Test
    void getAllUsers_shouldReturnUsers_whenUsersExist() throws Exception {
        List<User> users = List.of(
                new User("Иван", "example@email.com", 25),
                new User("Дмитрий", "examplesecond@email.com", 30)
        );
        ReflectionTestUtils.setField(users.get(0), "id", 1L);
        ReflectionTestUtils.setField(users.get(1), "id", 2L);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Иван"))
                .andExpect(jsonPath("$[0].email").value("example@email.com"))
                .andExpect(jsonPath("$[0].age").value(25))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Дмитрий"))
                .andExpect(jsonPath("$[1].email").value("examplesecond@email.com"))
                .andExpect(jsonPath("$[1].age").value(30));

        verify(userService).getAllUsers();
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(0)));

        verify(userService).getAllUsers();
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenRequestIsValid() throws Exception {
        User updatedUser = new User("Дмитрий", "examplesecond@email.com", 30);
        ReflectionTestUtils.setField(updatedUser, "id", 1L);
        String requestJson = """
                {
                "name": "Дмитрий",
                "email": "examplesecond@email.com",
                "age":30
                }
                """;

        when(userService.updateUser(
                1L,
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getAge()))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(updatedUser.getName()))
                .andExpect(jsonPath("$.email").value(updatedUser.getEmail()))
                .andExpect(jsonPath("$.age").value(updatedUser.getAge()));

        verify(userService).updateUser(1L, updatedUser.getName(), updatedUser.getEmail(), updatedUser.getAge());
    }

    @Test
    void updateUser_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        String requestJson = """
                {
                  "name": "",
                  "email": "invalid-email",
                  "age": -5
                }
                """;
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("age")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        User updatedUser = new User("Дмитрий", "examplesecond@example.com", 30);
        String requestJson = """
                {
                    "name": "Дмитрий",
                    "email": "examplesecond@example.com",
                    "age": 30
                }
                """;

        when(userService.updateUser(999L, updatedUser.getName(), updatedUser.getEmail(), updatedUser.getAge()))
                .thenThrow(new NoSuchElementException("Пользователь с id: 999 не найден"));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).updateUser(999L, updatedUser.getName(), updatedUser.getEmail(), updatedUser.getAge());
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new NoSuchElementException("Пользователь с id: 999 не найден"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).deleteUser(999L);
    }
}
