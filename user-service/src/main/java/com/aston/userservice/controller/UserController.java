package com.aston.userservice.controller;

import com.aston.userservice.dto.*;
import com.aston.userservice.entity.User;
import com.aston.userservice.mapper.UserMapper;
import com.aston.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(
        name = "Users",
        description = "REST API для управления пользователями"
)
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Operation(
            summary = "Создание пользователя",
            description = "Создает нового пользователя и возвращает его данные"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно создан",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserHateoasResponseDoc.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные пользователя",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<EntityModel<UserResponseDto>> createUser(@RequestBody @Valid UserRequestDto userRequestDto) {
        User user = userService.createUser(
                userRequestDto.name(),
                userRequestDto.email(),
                userRequestDto.age()
        );

        return ResponseEntity
                .created(URI.create("/api/users/" + user.getId()))
                .body(toEntityModel(user));
    }

    @Operation(summary = "Получение пользователя по ID",
            description = "Возвращает данные пользователя по его идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserHateoasResponseDoc.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный идентификатор пользователя",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponseDto>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);

        return ResponseEntity.ok(toEntityModel(user));
    }

    @Operation(
            summary = "Получение всех пользователей",
            description = "Возвращает список всех пользователей"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей получен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserCollectionHateoasResponseDoc.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<UserResponseDto>>> getAllUsers() {
        List<User> users = userService.getAllUsers();

        List<EntityModel<UserResponseDto>> entityModels = users.stream()
                .map(this::toEntityModel)
                .toList();

        CollectionModel<EntityModel<UserResponseDto>> collectionModel = CollectionModel.of(
                entityModels,
                linkTo(methodOn(UserController.class)
                        .getAllUsers()).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
    }

    @Operation(
            summary = "Обновление пользователя по ID",
            description = "Обновляет данные пользователя по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Обновлён",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserHateoasResponseDoc.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponseDto>> updateUser(@PathVariable Long id, @RequestBody @Valid UserRequestDto userRequestDto) {
        User user = userService.updateUser(
                id,
                userRequestDto.name(),
                userRequestDto.email(),
                userRequestDto.age());

        return ResponseEntity.ok(toEntityModel(user));
    }

    @Operation(
            summary = "Удаление пользователя по ID",
            description = "Удаляет пользователя по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Удален",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный ID",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponseDto> toEntityModel(User user) {
        UserResponseDto userResponseDto = userMapper.toResponseDto(user);

        return EntityModel.of(
                userResponseDto,
                linkTo(methodOn(UserController.class)
                        .getUserById(user.getId())).withSelfRel(),
                linkTo(methodOn(UserController.class)
                        .getAllUsers()).withRel("users")
        );
    }
}
