# Aston Microservices

Учебный проект на **Java 21 и Spring Boot 3.5.6**.

Приложение состоит из двух микросервисов: один отвечает за управление пользователями, второй — за отправку уведомлений по электронной почте.

Для обмена событиями между сервисами используется Apache Kafka. Данные хранятся в PostgreSQL.

## Возможности

- Создание, получение, обновление и удаление пользователей через REST API.
- Проверка входных данных и обработка ошибок.
- Отправка событий в Kafka при создании и удалении пользователя.
- Сохранение событий через Transactional Outbox.
- Повторная отправка событий при ошибках.
- Получение событий в notification-service.
- Защита от повторной обработки одинаковых событий.
- Отправка писем через SMTP.
- Повторные попытки отправки уведомлений.
- REST API для отправки произвольного письма.
- Запуск проекта через Docker Compose.

## Технологии

- Java 21
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA, Hibernate
- PostgreSQL 17
- Apache Kafka
- Spring Kafka
- Spring Mail
- Flyway
- Maven
- JUnit 5, Mockito, MockMvc
- Testcontainers 2.0.5
- Docker Compose
- Mailpit

## Структура проекта

Проект разделён на три Maven-модуля:

```text
aston-microservices/
├── event-contracts/
├── user-service/
├── notification-service/
├── docker-compose.yml
└── pom.xml
```

**event-contracts** — общие классы событий, которые используются обоими микросервисами.

**user-service** — REST API для работы с пользователями. При создании и удалении пользователя сохраняет событие в Outbox, после чего отправляет его в Kafka.

**notification-service** — получает события Kafka, сохраняет уведомления в PostgreSQL и отправляет письма.

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

При создании пользователя отправляется событие `USER_CREATED`, а при удалении — `USER_DELETED`.

Для повышения надёжности события сначала записываются в базу данных. Если Kafka временно недоступна, отправка повторяется.

Notification-service также сохраняет уведомления в БД и повторяет отправку письма в случае ошибки.

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

Проверка контейнеров:

```bash
docker compose ps
```

Просмотр логов:

```bash
docker compose logs -f user-service
docker compose logs -f notification-service
```

После запуска приложения доступны по адресам:

- User API: http://localhost:8080/api/users
- Notification API: http://localhost:8081/api/notifications/email
- Mailpit: http://localhost:8025

Mailpit используется для тестирования писем. Он принимает сообщения, но не отправляет их на реальные почтовые адреса.

## REST API пользователей

Базовый адрес: `http://localhost:8080/api/users`

| Метод | Адрес | Описание |
|---|---|---|
| POST | /api/users | Создание пользователя |
| GET | /api/users | Список пользователей |
| GET | /api/users/{id} | Поиск по ID |
| PUT | /api/users/{id} | Обновление пользователя |
| DELETE | /api/users/{id} | Удаление пользователя |

### Пример создания пользователя

```json
{
  "name": "Ivan",
  "email": "ivan@example.com",
  "age": 25
}
```

Пример запроса из PowerShell:

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

После создания пользователя формируется событие `USER_CREATED`. Оно передаётся через Kafka в notification-service, который создаёт и отправляет уведомление.

При удалении пользователя аналогичным образом формируется событие `USER_DELETED`.

## REST API уведомлений

Notification-service также позволяет отправить письмо напрямую, без Kafka.

**POST** `/api/notifications/email`

Пример JSON:

```json
{
  "to": "test@example.com",
  "subject": "Тестовое письмо",
  "body": "Привет! Это проверка отправки письма."
}
```

Пример запроса в PowerShell:

```powershell
$body = @{
    to = "test@example.com"
    subject = "Test email"
    body = "Hello from notification-service"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8081/api/notifications/email" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

При успешной отправке возвращается HTTP `202 Accepted`.

Отправленное письмо можно посмотреть в интерфейсе Mailpit:

http://localhost:8025

## Базы данных

Для каждого микросервиса используется отдельная база PostgreSQL.

**user-service:**
- `users` — пользователи.
- `outbox_events` — события для отправки в Kafka.

**notification-service:**
- `notifications` — уведомления и их статусы.
- `processed_user_events` — идентификаторы обработанных событий для защиты от дубликатов.

Для управления структурой таблиц используется Flyway.

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

Миграции применяются автоматически при запуске приложений.

### Проверка базы через Docker

```bash
docker compose exec user-postgres psql -U user_service_user -d user_service
```

```bash
docker compose exec notification-postgres psql -U notification_service_user -d notification_service
```

Примеры SQL-запросов:

```sql
SELECT * FROM users;

SELECT * FROM outbox_events;

SELECT * FROM notifications;
```

## Тестирование

В проекте используются JUnit 5, Mockito, MockMvc и Testcontainers.

Проверяются бизнес-логика, REST API, работа с PostgreSQL, публикация событий Kafka и другие компоненты.

Для запуска всех тестов из корня проекта:

```bash
mvn clean test
```

Для интеграционных тестов должен быть запущен Docker.

На последней проверке успешно прошли **97 тестов**:

| Модуль | Количество |
|---|---:|
| event-contracts | 5 |
| user-service | 86 |
| notification-service | 6 |
| **Всего** | **97** |

Результат сборки: `BUILD SUCCESS`.

Также вручную проверена работа двух микросервисов через Kafka, создание и удаление пользователей, сохранение уведомлений и отправка писем через Mailpit.

## Остановка проекта

Остановка контейнеров:

```bash
docker compose down
```

Данные PostgreSQL при этом сохраняются в Docker volumes.

Если нужно удалить также данные баз:

```bash
docker compose down -v
```

**Внимание:** команда с `-v` удаляет сохранённые данные PostgreSQL и Kafka.

## Локальный запуск

Для запуска без Docker-контейнеров приложений необходимы Java 21, Maven, а также доступные PostgreSQL и Kafka.

Микросервисы можно запускать отдельно через IntelliJ IDEA или Maven.

Для user-service:

```bash
mvn -pl user-service -am spring-boot:run
```

Для notification-service:

```bash
mvn -pl notification-service -am spring-boot:run
```

Для работы почтовых уведомлений также нужен SMTP-сервер. При локальном тестировании можно использовать Mailpit.

Основной способ запуска всего проекта — Docker Compose.