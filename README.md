# SmartPass Backend — подключение фронтенда

Практическая справка по подключению к API согласно текущей реализации репозитория. Базовый путь API — `/api/v1`. Примеры ниже предполагают локальный запуск.

## Запуск и конфигурация

Приложение использует Java 25, Spring Boot и MySQL. Порт `server.port` явно не задан, поэтому Spring Boot использует порт `8080` по умолчанию; `Dockerfile` также объявляет порт `8080`. Типичный базовый URL: `http://localhost:8080`; базовый URL API: `http://localhost:8080/api/v1`.

Для запуска из корня репозитория в PowerShell:

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "ваш-пароль-MySQL"
.\gradlew.bat bootRun
```

Для запуска в Bash:

```bash
export DB_USER=root
export DB_PASSWORD='ваш-пароль-MySQL'
./gradlew bootRun
```

Нужна база MySQL `SmartPass` на `localhost:3306`. Имя пользователя берётся из `DB_USER` (по умолчанию `root`), пароль — из обязательной переменной `DB_PASSWORD`; значения по умолчанию для пароля нет. URL базы задан в `src/main/resources/application.yml`; для другого адреса или имени БД конфигурацию потребуется изменить.

**Проверьте схему перед запуском.** Flyway настроен на `classpath:db/migration`, а SQL-файлы в репозитории находятся в `src/main/resources/db/migratoin` (в имени каталога опечатка). Hibernate настроен на `ddl-auto: validate`. Поэтому текущая конфигурация не гарантирует автоматическое создание схемы; подготовьте БД и проверьте, что её таблицы соответствуют сущностям приложения. Начальные SQL-данные содержат роли, зоны и точки доступа, но пользователя для входа не создают. Compose-файла и готовой инструкции запуска Docker-контейнера в репозитории нет.

## Аутентификация и доступ

Все запросы требуют **HTTP Basic Authentication**. Передавайте логин и пароль в заголовке `Authorization: Basic ...`. API работает без cookie-сессий, CSRF отключён; JWT-аутентификация не реализована. Пароль пользователя при создании сохраняется в виде BCrypt-хеша.

```http
Authorization: Basic <base64(login:password)>
```

Например, `curl` может сформировать заголовок:

```bash
curl -u 'login:password' http://localhost:8080/api/v1/users
```

Имя роли из таблицы `roles` становится authority `ROLE_<ИМЯ_РОЛИ>`. На маршрутах пользователей действуют ограничения:

| Маршруты | Требуемая роль |
|---|---|
| `GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/users/search`, `GET /api/v1/users/zone/{zoneId}` | `ADMIN` или `SECURITY` |
| `POST /api/v1/users`, `PUT /api/v1/users/{id}/unblock` | `ADMIN` |
| `PUT /api/v1/users/{id}/block` | `ADMIN` или `SECURITY` |

Для остальных маршрутов ниже дополнительные проверки ролей в контроллерах не заданы, но действует общее требование аутентификации. Начальные данные содержат роли `EMPLOYEE`, `SECURITY`, `ADMIN`. Учётные данные первого пользователя и способ его создания вне API в репозитории не определены.

## API

Запросы с телом принимают JSON. Enum-значения передаются строками в верхнем регистре. Поля типа `LocalDateTime` принимают строки вида `2026-10-16T14:30:00`, без указания часового пояса.

### Пользователи — `/api/v1/users`

| Метод и путь | Параметры | Ответ |
|---|---|---|
| `GET /{id}` | `id` — ID пользователя | `UserResponse` |
| `GET /` | `page` (по умолчанию `0`), `size` (по умолчанию `10`, ограничивается диапазоном `1–100`), `sortBy` (по умолчанию `lastName`) | Страница пользователей, сортировка по возрастанию |
| `GET /search` | обязательный `name`; `page` (`0`), `size` (`10`, максимум `100`) | Страница результатов поиска по ФИО |
| `GET /zone/{zoneId}` | `zoneId` — ID зоны | Массив пользователей |
| `POST /` | JSON с данными нового пользователя | `201 Created`, `UserResponse` |
| `PUT /{id}/block` | JSON `{"reason":"..."}` | `204 No Content` |
| `PUT /{id}/unblock` | `id`, тело не требуется | `204 No Content` |

Для `GET /` поддерживаются значения `sortBy`: `id`, `lastName`, `firstName`, `position`, `status`.

Пример создания пользователя:

```http
POST /api/v1/users
Content-Type: application/json

{
  "roleId": 1,
  "zoneId": 1,
  "firstName": "Анна",
  "lastName": "Иванова",
  "middleName": "Петровна",
  "numberPhone": "+70000000000",
  "email": "anna@example.test",
  "position": "Инженер",
  "address": "Москва",
  "birthday": "1990-04-12T00:00:00",
  "stage": "2020-01-15T00:00:00",
  "login": "anna.ivanova",
  "password": "example-password"
}
```

`roleId` и `zoneId` должны существовать в БД. DTO требует непустые имя, фамилию, телефон, email, должность, адрес и логин; пароль должен содержать от 8 до 72 символов, дата рождения — быть в прошлом. `middleName` может отсутствовать. DTO допускает email до 100 символов, но SQL-миграция задаёт столбцу `email` длину 30 символов; до согласования схемы используйте адрес не длиннее 30 символов.

Пример `UserResponse`:

```json
{
  "id": 12,
  "roleName": "EMPLOYEE",
  "zoneId": 1,
  "fullName": "Иванова Анна Петровна",
  "numberPhone": "+70000000000",
  "email": "anna@example.test",
  "position": "Инженер",
  "photoUrl": null,
  "status": "ACTIVE",
  "blockingReason": null
}
```

`status` принимает значения `ACTIVE` или `BLOCKED`. Неподдерживаемый `sortBy` возвращает `400 Bad Request`. Отрицательный `page` приводится к `0`, `size` ограничивается диапазоном `1–100`.

### Гостевые пропуска — `/api/v1/guest-passes`

| Метод и путь | Параметры | Ответ |
|---|---|---|
| `POST /` | JSON с `creatorUserId`, `guestName`, `validFrom`, `validTo` | `201 Created`, `GuestPassResponse` |
| `PUT /{id}/activate` | `id`; тело не требуется | Успешный ответ без JSON-тела |
| `GET /search/uid` | обязательный query-параметр `uid` | Один пропуск |
| `GET /search/name` | обязательный query-параметр `name` | Массив пропусков |
| `GET /user/{userId}` | `page` (по умолчанию `0`), `size` (по умолчанию `10`) | Страница пропусков сотрудника |

Пример запроса:

```http
POST /api/v1/guest-passes
Content-Type: application/json

{
  "creatorUserId": 12,
  "guestName": "Пётр Сидоров",
  "validFrom": "2026-10-17T09:00:00",
  "validTo": "2026-10-17T18:00:00"
}
```

`GuestPassResponse` включает `id`, `creatorUserId`, `creatorFullName`, `guestName`, `validFrom`, `validTo`, `isActivated`, `uidLink`. `uidLink` генерируется сервером; конкретное значение заранее неизвестно. Сервис проверяет, что `validTo` не раньше `validFrom`. Поля запроса не размечены `@Valid`, но сервис ожидает заполненные значения.

### Точки доступа — `/api/v1/checkpoints`

| Метод и путь | Параметры | Ответ |
|---|---|---|
| `GET /` | — | Массив `CheckpointResponse` |
| `GET /{id}` | `id` — ID точки доступа | Один чекпоинт |
| `GET /zone/{zoneId}` | `zoneId` — ID зоны | Массив чекпоинтов зоны |
| `POST /` | JSON `{"name":"Главное КПП","zoneId":1}` | `201 Created`, созданный чекпоинт |
| `PUT /{id}/status` | query-параметр `status`: `ONLINE` или `OFFLINE` | Обновлённый чекпоинт |

`CheckpointResponse`: `id`, `name`, `zoneId`, `zoneName`, `status`.

```http
PUT /api/v1/checkpoints/3/status?status=OFFLINE
```

### QR-токены — `/api/v1/qr`

| Метод и путь | Параметры | Ответ |
|---|---|---|
| `POST /generate/{userId}` | `userId` — ID пользователя | `201 Created`, объект токена |
| `GET /status/{userId}` | `userId` — ID пользователя | Текущий активный токен |

Пример ответа:

```json
{
  "token": "сгенерированное сервером значение",
  "expiresAt": "2026-10-16T14:03:05"
}
```

Сервис задаёт срок действия токена в 30 секунд. Тело запроса не требуется. QR-токен используется для проверки прохода и не заменяет HTTP Basic-аутентификацию API.

### Проверка прохода — `/api/v1/access-check`

`POST /verify` принимает:

```json
{
  "checkpointId": 1,
  "direction": "IN",
  "scannedCode": "считанный QR-токен или UID гостевого пропуска"
}
```

`direction` — `IN` или `OUT`. При обработанном запросе сервер возвращает `200 OK` с результатом проверки, в том числе при отказе:

```json
{
  "isAllowed": false,
  "message": "Причина отказа"
}
```

Отказ доступа — результат проверки, а не HTTP-ошибка. Если `checkpointId`, `direction` или `scannedCode` отсутствуют либо равны `null`, контроллер возвращает `400 Bad Request` с объектом того же формата:

```json
{
  "isAllowed": false,
  "message": "Неверный формат запроса. Все поля (checkpointId, direction, scannedCode) обязательны."
}
```

Пустая строка в `scannedCode` отдельно контроллером не проверяется.

### Журнал проходов — `/api/v1/access-logs`

| Метод и путь | Параметры | Ответ |
|---|---|---|
| `GET /` | `page` (`0`), `size` (`20`), `sortBy` (`scannedAt`) | Страница событий, сортировка по убыванию |
| `GET /user/{userId}` | `page` (`0`), `size` (`20`) | Страница событий сотрудника, сортировка по `id` по убыванию |
| `GET /checkpoint/{checkpointId}` | `checkpointId` | Массив событий чекпоинта |
| `GET /status` | обязательный `status`: `ALLOWED` или `DENIED` | Массив событий с указанным статусом |
| `GET /time-range` | обязательные `start`, `end` в ISO `LocalDateTime` | Массив событий за период |

Примеры:

```http
GET /api/v1/access-logs?page=0&size=20&sortBy=scannedAt
GET /api/v1/access-logs/status?status=DENIED
GET /api/v1/access-logs/time-range?start=2026-10-16T09:00:00&end=2026-10-16T18:00:00
```

Элемент `AccessLogResponse` содержит `id`, `checkpointId`, `checkpointName`, `zoneName`, `userId`, `userFullName`, `guestPassId`, `guestName`, `direction`, `status`, `denyReason`, `scannedAt`. Поля пользователя или гостя зависят от типа прохода.

### Пагинация

Методы, возвращающие Spring Data `Page`, отдают объект со списком элементов `content` и метаданными страницы, включая `number`, `size`, `totalElements` и `totalPages`. Значения по умолчанию зависят от эндпоинта и указаны в таблицах выше.

## Ошибки и CORS

Обработанные приложением ошибки возвращаются в формате Spring `ProblemDetail`. В ответ добавляется поле `timestamp`; для ошибок валидации также добавляется объект `errors` с именами полей и сообщениями.

```json
{
  "type": "about:blank",
  "title": "Ошибка валидации",
  "status": 400,
  "detail": "Ошибка валидации",
  "timestamp": "2026-10-16T14:00:00Z",
  "errors": {
    "email": "сообщение об ошибке"
  }
}
```

Основные статусы, заданные обработчиком ошибок:

| HTTP | Значение |
|---|---|
| `400 Bad Request` | Ошибка валидации или неверный аргумент |
| `401 Unauthorized` | Нет действительной HTTP Basic-аутентификации |
| `403 Forbidden` | Нет требуемой роли |
| `404 Not Found` | Не найден пользователь или роль; сервисы также используют это исключение для некоторых отсутствующих ресурсов |
| `409 Conflict` | Дубликат пользователя/данных или конфликт бизнес-логики |

Ответы `401` и `403` формирует Spring Security, а не `ApiExceptionHandler`; их JSON-формат отдельно не настроен. Формат ошибок, не перечисленных выше, обработчиком не фиксируется.

CORS настроен для всех путей. Разрешены методы `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` и любые заголовки. В `SecurityConfig` значение origin по умолчанию — `http://localhost:3000`.

**Несовпадение в конфигурации:** в `application.yml` указан ключ `spring.app.cors.allowed-origins`, а код читает верхнеуровневое свойство `app.cors.allowed-origins`. Это разные свойства. Для другого origin передавайте верхнеуровневый ключ, например:

```bash
./gradlew bootRun --args='--app.cors.allowed-origins=http://localhost:3000,https://frontend.example'
```

CORS с credentials отдельно не включён.

## Полезные примечания

- Проверяйте `isAllowed` в ответе проверки прохода: статус `200 OK` может означать отказ в доступе.
- Роли, ID пользователей и ID зон зависят от БД; проверьте реальные значения в используемом окружении.
- Даты `LocalDateTime` не содержат часового пояса.
- В API не описаны маршруты для получения списка ролей или зон; не предполагайте, что такие URL существуют.
- Адрес сервера, учётные данные и готовность схемы БД зависят от окружения и не определяются полностью репозиторием.
