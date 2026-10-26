package com.aston.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Представление пользователя с HATEOAS-ссылками")
public record UserHateoasResponseDoc(
        Long id,
        String name,
        String email,
        Integer age,
        LocalDateTime createdAt,

        @Schema(description = "HATEOAS-ссылки на связанные ресурсы")
        Map<String, LinkDoc> _links
) {
    public record LinkDoc(String href) {

    }
}
