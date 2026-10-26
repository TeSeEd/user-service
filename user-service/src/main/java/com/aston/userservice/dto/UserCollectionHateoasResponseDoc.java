package com.aston.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Коллекция пользователей с HATEOAS-ссылками")
public record UserCollectionHateoasResponseDoc(
        @Schema(description = "Пользователи, входящие в коллекцию")
        EmbeddedDoc _embedded,

        @Schema(description = "HATEOAS-ссылки коллекции")
        Map<String, UserHateoasResponseDoc.LinkDoc> _links
) {
    public record EmbeddedDoc(
            List<UserHateoasResponseDoc> userResponseDtoList
    ) {
    }
}
