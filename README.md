# Aston Microservices

Учебный проект на **Java 21 и Spring Boot 3.5.6**.

Проект состоит из двух микросервисов: `user-service` для управления пользователями и `notification-service` для отправки уведомлений по электронной почте.

Для обмена событиями используется Apache Kafka. Данные хранятся в PostgreSQL.

Для примера используется название сайта **AstonHub**.

## Возможности

- Создание, получение, обновление и удаление пользователей через REST API.
- Валидация данных и обработка ошибок.
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

**user-service** — REST API пользователей. При создании и удалении пользователя сохраняет событие в Outbox, после чего публикует его в Kafka.

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
- Mailpit: http://localhost:8025

Mailpit используется для тестирования отправки писем. Сообщения принимаются локально и не отправляются на реальные почтовые адреса.

## REST API пользователей

Базовый адрес: `http://localhost:8080/api/users`

| Метод | Endpoint | Описание |
|---|---|---|
| POST | /api/users | Создание пользователя |
| GET | /api/users | Список пользователей |
| GET | /api/users/{id} | Получение по ID |
| PUT | /api/users/{id} | Обновление пользователя |
| DELETE | /api/users/{id} | Удаление пользователя |

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

После создания пользователя формируется событие `USER_CREATED`, которое передаётся через Kafka в notification-service.

При удалении пользователя аналогичным образом формируется событие `USER_DELETED`.

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

При успешной отправке возвращается HTTP `202 Accepted`.

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
- REST API.
- Работа с PostgreSQL.
- Сохранение событий в Outbox.
- Публикация сообщений Kafka.
- Работа репозитория уведомлений.
- Отправка электронных писем.

Для интеграционных тестов почты используется **GreenMail** — тестовый SMTP-сервер.

Добавлены три сценария:

1. Отправка приветственного письма при `USER_CREATED`.
2. Отправка письма при `USER_DELETED`.
3. Отправка произвольного письма через REST API.

Тесты проверяют получателя, тему и содержимое сообщения.

### Запуск всех тестов

Из корня проекта:

```bash
mvn clean test
```

Для тестов с Testcontainers необходим работающий Docker.

На последнем полностью успешном запуске прошли **97 тестов**:

| Модуль | Количество |
|---|---:|
| event-contracts | 5 |
| user-service | 86 |
| notification-service | 6 |
| **Всего** | **97** |

Дополнительно добавлены три интеграционных теста SMTP. На первом запуске два теста прошли, а третий обнаружил несовпадение ожидаемой темы письма. После исправления требуется повторный запуск тестов.

### Отдельный запуск тестов SMTP

В Windows PowerShell:

```powershell
mvn -pl notification-service -am "-Dtest=EmailIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Кроме автоматических тестов, вручную проверены создание и удаление пользователей, публикация Kafka-событий, сохранение уведомлений и отправка писем через Mailpit.

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