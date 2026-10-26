# Aston Microservices

Учебный проект на **Java 21 и Spring Boot 3.5.6**.

Проект состоит из двух микросервисов: `user-service` для управления пользователями и `notification-service` для отправки уведомлений по электронной почте.

Для обмена событиями используется Apache Kafka. Данные хранятся в PostgreSQL.

Для примера используется название сайта **AstonHub**.

## Возможности

- Создание, получение, обновление и удаление пользователей через REST API.
- Валидация данных и централизованная обработка ошибок.
- Документирование REST API с помощью Springdoc OpenAPI и Swagger UI.
- Поддержка HATEOAS и HAL для навигации по ресурсам пользователей.
- Отправка событий Kafka при создании и удалении пользователя.
- Transactional Outbox для надёжной публикации событий.
- Повторные попытки отправки событий при ошибках.
- Получение и обработка Kafka-событий.
- Защита от повторной обработки одинаковых событий.
- Отправка уведомлений через SMTP.
- Повторные попытки отправки писем.
- REST API для отправки произвольного письма.
- Запуск проекта через Docker Compose.

## Технологии

- Java 21
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA, Hibernate
- Springdoc OpenAPI, Swagger UI
- Spring HATEOAS
- PostgreSQL 17
- Apache Kafka, Spring Kafka
- Spring Mail
- Flyway
- Maven
- JUnit 5, Mockito, MockMvc
- Testcontainers 2.0.5
- GreenMail
- Docker Compose
- Mailpit

## Структура проекта

Проект состоит из трёх Maven-модулей:

```text
aston-microservices/
├── event-contracts/
├── user-service/
├── notification-service/
├── docker-compose.yml
└── pom.xml
```

**event-contracts** — общие классы событий для обмена сообщениями между сервисами.

**user-service** — REST API пользователей с документацией OpenAPI и поддержкой HATEOAS. При создании и удалении пользователя сохраняет событие в Outbox, после чего публикует его в Kafka.

**notification-service** — обрабатывает события Kafka, сохраняет уведомления в PostgreSQL и отправляет письма.

### Как работают уведомления

```text
REST API
   |
   v
user-service
   |
   v
PostgreSQL (Outbox)
   |
   v
Apache Kafka
   |
   v
notification-service
   |
   v
PostgreSQL (Notifications)
   |
   v
SMTP
```

При создании пользователя формируется событие `USER_CREATED`, при удалении — `USER_DELETED`.

События сначала сохраняются в базе данных. Если Kafka недоступна, публикация повторяется.

Notification-service сохраняет уведомления и также выполняет повторные попытки отправки при ошибках SMTP.

### Содержание писем

При создании пользователя отправляется приветственное письмо:

**Тема:** Добро пожаловать в AstonHub!

**Текст:**
> Здравствуйте! Ваш аккаунт на сайте AstonHub был успешно создан.

При удалении пользователя:

**Тема:** Удаление аккаунта в AstonHub

**Текст:**
> Здравствуйте! Ваш аккаунт на сайте AstonHub был удалён.

## Запуск через Docker

Для запуска необходимы Docker Desktop и Docker Compose.

Из корневой папки проекта выполнить:

```bash
docker compose up -d --build
```

Docker Compose запускает шесть сервисов:

| Сервис | Назначение | Порт |
|---|---|---|
| user-service | REST API пользователей | 8080 |
| notification-service | REST API уведомлений | 8081 |
| user-postgres | База пользователей | 5432 |
| notification-postgres | База уведомлений | 5433 |
| kafka | Брокер сообщений | 9092 |
| mailpit | Тестовый SMTP-сервер | 1025, 8025 |

Проверка состояния контейнеров:

```bash
docker compose ps
```

Просмотр логов:

```bash
docker compose logs -f user-service
docker compose logs -f notification-service
```

После запуска доступны:

- User API: http://localhost:8080/api/users
- Notification API: http://localhost:8081/api/notifications/email
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Mailpit: http://localhost:8025

Mailpit используется для тестирования отправки писем. Сообщения принимаются локально и не отправляются на реальные почтовые адреса.

## REST API пользователей

Базовый адрес: `http://localhost:8080/api/users`

| Метод | Endpoint | Описание | Успешный статус |
|---|---|---|---|
| POST | /api/users | Создание пользователя | 201 Created |
| GET | /api/users | Список пользователей | 200 OK |
| GET | /api/users/{id} | Получение по ID | 200 OK |
| PUT | /api/users/{id} | Обновление пользователя | 200 OK |
| DELETE | /api/users/{id} | Удаление пользователя | 204 No Content |

Для некорректных данных предусмотрены ответы `400 Bad Request`, для отсутствующего пользователя — `404 Not Found`.

Ответы с ошибками имеют структуру:

```json
{
  "status": 404,
  "message": "Пользователь с id: 999 не найден",
  "timestamp": "2026-10-26T12:00:00"
}
```

### Создание пользователя

Пример JSON:

```json
{
  "name": "Ivan",
  "email": "ivan@example.com",
  "age": 25
}
```

Пример запроса через PowerShell:

```powershell
$body = @{
    name = "Ivan"
    email = "ivan@example.com"
    age = 25
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/users" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

При успешном создании возвращается `201 Created`, заголовок `Location` со ссылкой на созданного пользователя и JSON с его данными и HATEOAS-ссылками.

После создания пользователя формируется событие `USER_CREATED`, которое передаётся через Kafka в notification-service.

При удалении пользователя аналогичным образом формируется событие `USER_DELETED`.

## Swagger и OpenAPI

Для документирования REST API в `user-service` используется **Springdoc OpenAPI**.

Документация автоматически формируется на основе Spring MVC-контроллера, DTO и аннотаций OpenAPI.

Интерактивный интерфейс доступен по адресу:

**http://localhost:8080/swagger-ui.html**

Машиночитаемое описание OpenAPI в JSON:

**http://localhost:8080/v3/api-docs**

Документация содержит:

- Описания всех пяти CRUD-операций.
- Параметры и тела запросов.
- HTTP-статусы успешных ответов и ошибок.
- Схемы `UserRequestDto` и `ErrorResponseDto`.
- Схемы HATEOAS-ответов отдельного пользователя и коллекции пользователей.

Для описания HATEOAS-формата используются отдельные документационные модели:

- `UserHateoasResponseDoc`
- `UserCollectionHateoasResponseDoc`

Они используются для генерации документации и не заменяют реальные DTO или объекты Spring HATEOAS.

## HATEOAS и HAL

REST API пользователей поддерживает **HATEOAS (Hypermedia as the Engine of Application State)** с использованием Spring HATEOAS.

Вместе с данными API возвращает ссылки на доступные ресурсы. Клиент может использовать эти ссылки для навигации по API.

Для формирования ответов применяются:

- `EntityModel<UserResponseDto>` — представление отдельного пользователя со ссылками.
- `CollectionModel<EntityModel<UserResponseDto>>` — представление коллекции пользователей.
- `WebMvcLinkBuilder` — построение ссылок на методы контроллера.

### Получение пользователя

**GET** `/api/users/1`

Пример ответа:

```json
{
  "id": 1,
  "name": "Ivan",
  "email": "ivan@example.com",
  "age": 25,
  "createdAt": "2026-10-26T12:00:00",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users/1"
    },
    "users": {
      "href": "http://localhost:8080/api/users"
    }
  }
}
```

Ссылка `self` указывает на текущего пользователя.

Ссылка `users` позволяет перейти к коллекции всех пользователей.

### Получение списка пользователей

**GET** `/api/users`

Пример ответа:

```json
{
  "_embedded": {
    "userResponseDtoList": [
      {
        "id": 1,
        "name": "Ivan",
        "email": "ivan@example.com",
        "age": 25,
        "createdAt": "2026-10-26T12:00:00",
        "_links": {
          "self": {
            "href": "http://localhost:8080/api/users/1"
          },
          "users": {
            "href": "http://localhost:8080/api/users"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users"
    }
  }
}
```

Поле `_embedded` содержит пользователей, каждый из которых имеет собственные HATEOAS-ссылки.

Поле `_links` на верхнем уровне описывает ссылки самой коллекции.

Если пользователей нет, коллекция возвращается с HTTP `200 OK` и ссылкой `self`; блок `_embedded` может отсутствовать.

**Важно:** после внедрения HATEOAS метод `GET /api/users` возвращает HAL-объект, а не корневой JSON-массив.

### HATEOAS в остальных операциях

- `POST /api/users` — `201 Created`, данные созданного пользователя со ссылками `self` и `users`.
- `PUT /api/users/{id}` — `200 OK`, обновлённый пользователь со ссылками `self` и `users`.
- `DELETE /api/users/{id}` — `204 No Content`, без тела ответа.

Бизнес-логика сервисного слоя при внедрении HATEOAS не изменялась.

## REST API уведомлений

Notification-service позволяет отправить письмо напрямую, без Kafka.

**POST** `/api/notifications/email`

Пример JSON:

```json
{
  "to": "test@example.com",
  "subject": "Тестовое письмо",
  "body": "Привет! Это проверка отправки письма."
}
```

Пример запроса через PowerShell:

```powershell
$body = @{
    to = "test@example.com"
    subject = "Test email"
    body = "Hello from AstonHub"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8081/api/notifications/email" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

При успешном принятии запроса возвращается HTTP `202 Accepted`.

Письмо можно проверить в Mailpit:

http://localhost:8025

## Базы данных

Для каждого микросервиса используется отдельная база PostgreSQL.

**user-service:**

- `users` — пользователи.
- `outbox_events` — события для отправки в Kafka.

**notification-service:**

- `notifications` — уведомления и их статусы.
- `processed_user_events` — обработанные события для защиты от дубликатов.

Для управления схемой базы данных используется Flyway. Миграции применяются автоматически при запуске приложений.

### Подключение к PostgreSQL

База пользователей:

```bash
docker compose exec user-postgres psql -U user_service_user -d user_service
```

База уведомлений:

```bash
docker compose exec notification-postgres psql -U notification_service_user -d notification_service
```

Примеры SQL-запросов:

```sql
SELECT * FROM users;
SELECT * FROM outbox_events;
SELECT * FROM notifications;
```

Каждый запрос нужно выполнять в соответствующей базе данных.

## Тестирование

В проекте используются JUnit 5, Mockito, MockMvc и Testcontainers.

Проверяются:

- Бизнес-логика работы с пользователями.
- REST API и валидация данных.
- HTTP-статусы и обработка ошибок.
- HATEOAS-ссылки в ответах API.
- HAL-коллекции, включая пустой список пользователей.
- Работа с PostgreSQL.
- Сохранение событий в Transactional Outbox.
- Публикация Kafka-событий.
- Обработка событий и защита от дубликатов.
- Транзакционность сохранения обработанных событий.
- Работа репозитория уведомлений.
- Отправка электронных писем.

### Тестирование HATEOAS

В `UserControllerTest` используются MockMvc и Mockito.

Тесты проверяют:

- `POST /api/users` — статус `201`, заголовок `Location`, поля пользователя, ссылки `self` и `users`.
- `GET /api/users/{id}` — данные пользователя и ссылки `self` и `users`.
- `GET /api/users` — структуру `_embedded.userResponseDtoList`, данные пользователей и HATEOAS-ссылки.
- Пустой список — HTTP `200`, отсутствие `_embedded` и наличие ссылки коллекции `self`.
- `PUT /api/users/{id}` — обновлённые данные и HATEOAS-ссылки.
- `DELETE /api/users/{id}` — статус `204` и пустое тело.
- Обработку некорректных запросов и отсутствующих пользователей.

Для запуска тестов контроллера из корня проекта:

```powershell
mvn -pl user-service -am "-Dtest=UserControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### Интеграционные тесты SMTP

Для интеграционных тестов почты используется **GreenMail** — тестовый SMTP-сервер.

Проверяются три сценария:

1. Отправка приветственного письма при `USER_CREATED`.
2. Отправка письма при `USER_DELETED`.
3. Отправка произвольного письма через REST API.

Тесты проверяют получателя, тему и содержимое сообщения.

Отдельный запуск в Windows PowerShell:

```powershell
mvn -pl notification-service -am "-Dtest=EmailIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### Запуск всех тестов

Из корня проекта:

```bash
mvn clean test
```

Для интеграционных тестов с Testcontainers необходим работающий Docker.

**Последний подтверждённый успешный прогон: 26 октября 2026 года.**

```text
[INFO] Reactor Summary:
[INFO]
[INFO] event-contracts 1.0.0 ............... SUCCESS
[INFO] user-service 1.0-SNAPSHOT ........... SUCCESS
[INFO] notification-service 0.0.1-SNAPSHOT . SUCCESS
[INFO] aston-microservices 1.0.0 ........... SUCCESS
[INFO]
[INFO] BUILD SUCCESS
[INFO] Total time: 01:13 min
```

Все модули успешно прошли полный цикл `mvn clean test`.

Кроме автоматических тестов, вручную проверены REST API, создание и удаление пользователей, Kafka-события, сохранение уведомлений и отправка писем через Mailpit.

Также проверены Swagger UI, OpenAPI JSON и реальные HAL-ответы API пользователей.

## Остановка проекта

Остановка контейнеров:

```bash
docker compose down
```

Данные PostgreSQL сохраняются в Docker volumes.

Для остановки с удалением данных:

```bash
docker compose down -v
```

**Внимание:** параметр `-v` удаляет данные в Docker volumes, используемых проектом.

## Локальный запуск

Для локального запуска необходимы Java 21, Maven, PostgreSQL и Kafka.

Микросервисы можно запускать отдельно через IntelliJ IDEA или Maven с соответствующими настройками подключения к инфраструктуре.

Для работы уведомлений также необходим SMTP-сервер. При разработке можно использовать Mailpit.

Основной способ запуска всего проекта — Docker Compose.