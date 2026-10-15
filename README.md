# User Service

REST API для управления пользователями, разработанный на **Java 21 и Spring Boot 3.5.6**.

Приложение реализует CRUD-операции через HTTP, использует Spring Data JPA и Hibernate ORM для работы с PostgreSQL 17.

Проект поддерживает локальный запуск и запуск через Docker Compose. При запуске через Docker автоматически поднимаются два контейнера: Spring Boot-приложение и PostgreSQL.

## Возможности

- Создание пользователей.
- Поиск пользователя по ID.
- Получение списка всех пользователей.
- Обновление информации о пользователе.
- Удаление пользователей.
- Валидация входных данных.
- Централизованная обработка ошибок.
- Управление транзакциями.
- Автоматическое создание и обновление таблиц Hibernate.
- Модульное и интеграционное тестирование.

## Используемые технологии

- Java 21.
- Spring Boot 3.5.6.
- Spring Web MVC.
- Spring Data JPA.
- Hibernate ORM.
- Jakarta Bean Validation.
- PostgreSQL 17.
- Maven.
- JUnit 5.
- Mockito.
- MockMvc.
- Testcontainers 2.0.5.
- Docker и Docker Compose.

## Архитектура приложения

Проект разделён на следующие слои:

| Слой | Назначение |
|---|---|
| Controller | Обработка HTTP-запросов и формирование ответов |
| Service | Бизнес-логика, проверки и управление транзакциями |
| Repository | Работа с базой данных через Spring Data JPA |
| Entity | JPA-сущность пользователя |
| DTO | Передача данных между клиентом и REST API |
| Mapper | Преобразование сущностей в DTO |
| Exception Handler | Централизованная обработка исключений |

В качестве JPA-провайдера используется Hibernate ORM.

### Сущность User

| Поле | Тип | Назначение |
|---|---|---|
| id | Long | Уникальный идентификатор пользователя |
| name | String | Имя |
| email | String | Электронная почта |
| age | Integer | Возраст |
| createdAt | LocalDateTime | Дата и время создания |

Поле `id` генерируется автоматически. Поле `createdAt` заполняется при создании пользователя.

## Требования для запуска

Для запуска приложения через Docker необходимы:

- Docker Desktop или Docker Engine.
- Docker Compose.

Отдельная установка Java, Maven и PostgreSQL не требуется.

## Запуск через Docker Compose

Откройте терминал в корневом каталоге проекта.

### Сборка и запуск

```shell
docker compose up --build -d
```

Docker Compose соберёт Spring Boot-приложение и запустит два контейнера:

| Контейнер | Назначение |
|---|---|
| user-service-postgres | PostgreSQL 17 |
| user-service-app | Spring Boot REST API |

Приложение запускается после прохождения проверки готовности PostgreSQL.

### Проверка состояния

```shell
docker compose ps
```

### Просмотр логов Spring Boot

```shell
docker compose logs -f user-service
```

### Проверка REST API

После запуска приложение доступно по адресу:

```text
http://localhost:8080/api/users
```

Проверить работу можно через браузер или терминал:

```shell
curl http://localhost:8080/api/users
```

Если в базе данных ещё нет пользователей, ответом будет:

```json
[]
```

В Windows PowerShell для HTTP-запросов можно использовать `curl.exe` или `Invoke-RestMethod`.

## REST API

Базовый URL:

```text
http://localhost:8080/api/users
```

| Метод | Endpoint | Описание | HTTP-статус |
|---|---|---|---|
| POST | /api/users | Создать пользователя | 201 Created |
| GET | /api/users | Получить список пользователей | 200 OK |
| GET | /api/users/{id} | Получить пользователя по ID | 200 OK |
| PUT | /api/users/{id} | Обновить пользователя | 200 OK |
| DELETE | /api/users/{id} | Удалить пользователя | 204 No Content |

### Создание пользователя

**POST** `/api/users`

Тело запроса:

```json
{
  "name": "Иван",
  "email": "ivan@example.com",
  "age": 25
}
```

При успешном создании возвращается HTTP 201, данные пользователя и заголовок `Location`.

### Получение пользователя

**GET** `/api/users/1`

Возвращает данные пользователя с ID 1.

Если пользователь не найден, возвращается HTTP 404.

### Получение списка пользователей

**GET** `/api/users`

Возвращает массив пользователей. Если пользователей нет, возвращается пустой массив.

### Обновление пользователя

**PUT** `/api/users/1`

Тело запроса:

```json
{
  "name": "Дмитрий",
  "email": "dmitry@example.com",
  "age": 30
}
```

При успешном обновлении возвращается HTTP 200 и обновлённый пользователь.

### Удаление пользователя

**DELETE** `/api/users/1`

При успешном удалении возвращается HTTP 204 без тела ответа.

## Работа с API через PowerShell

### Создание пользователя

```powershell
$body = @{
    name = "Иван"
    email = "ivan@example.com"
    age = 25
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

### Получение всех пользователей

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method Get
```

### Поиск пользователя

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users/1" -Method Get
```

### Обновление пользователя

```powershell
$body = @{
    name = "Дмитрий"
    email = "dmitry@example.com"
    age = 30
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/users/1" -Method Put -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

### Удаление пользователя

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users/1" -Method Delete
```

Вместо `1` необходимо указывать существующий идентификатор пользователя.

## Валидация данных

Для проверки входных данных используется Jakarta Bean Validation.

- `name` — не должен быть пустым.
- `email` — должен быть непустым адресом корректного формата.
- `age` — должен быть положительным числом.

Некорректные данные приводят к ответу `400 Bad Request`.

Для обработки исключений используется `GlobalExceptionHandler`.

Основные статусы ошибок:

| Статус | Причина |
|---|---|
| 400 Bad Request | Некорректные входные данные или ID |
| 404 Not Found | Пользователь не найден |

Ответы об ошибках формируются в виде JSON со статусом, сообщением и временем возникновения.

## База данных

Для хранения пользователей используется PostgreSQL 17.

При запуске через Docker Compose приложение подключается к БД по внутреннему адресу:

```text
jdbc:postgresql://postgres:5432/user_service
```

`postgres` — сетевое имя сервиса базы данных внутри Docker Compose.

При локальном запуске используется адрес:

```text
jdbc:postgresql://localhost:5432/user_service
```

Параметры подключения Spring Boot могут переопределяться переменными окружения.

Схемой базы данных управляет Hibernate с настройкой:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Данные сохраняются в Docker volume `postgres_data` и не удаляются при обычной остановке контейнеров.

### Подключение к PostgreSQL через терминал

```shell
docker compose exec postgres psql -U user_service_user -d user_service
```

Пример SQL-запроса:

```sql
SELECT * FROM users;
```

Для выхода из PostgreSQL:

```text
\q
```

## Тестирование

В проекте реализованы модульные, MVC и интеграционные тесты.

| Тестовый класс | Назначение | Количество |
|---|---|---|
| UserServiceTest | Бизнес-логика с Mockito | 36 |
| UserControllerTest | REST API с MockMvc | 11 |
| UserRepositoryIntegrationTest | PostgreSQL через Testcontainers | 7 |

**Всего: 54 теста.**

Последняя проверка Maven завершилась успешно:

```text
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Запуск тестов

Для запуска тестов локально необходимы Java, Maven и работающий Docker Engine.

```shell
mvn clean test
```

Testcontainers создаёт отдельный временный контейнер PostgreSQL для интеграционных тестов.

Данные основной базы приложения при этом не изменяются.

## Логи

Просмотр логов приложения:

```shell
docker compose logs -f user-service
```

Просмотр логов PostgreSQL:

```shell
docker compose logs -f postgres
```

Вывод SQL-запросов Hibernate в стандартную консоль приложения отключён.

## Остановка приложения

Остановить и удалить контейнеры приложения:

```shell
docker compose down
```

Данные PostgreSQL сохраняются.

Остановить контейнеры и удалить данные базы:

```shell
docker compose down -v
```

**Внимание:** команда с `-v` удаляет Docker volume с данными PostgreSQL.

## Локальный запуск

При наличии Java 21, Maven и доступного PostgreSQL приложение можно запустить непосредственно на компьютере:

```shell
mvn spring-boot:run
```

По умолчанию REST API использует порт 8080.

Локальный запуск полезен для разработки и отладки приложения в IntelliJ IDEA.