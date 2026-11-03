# Локальный сервер SmartPass

Сервер на встроенных модулях Node.js раздаёт файлы из `Frontend/` и проксирует запросы `/api/*` в существующий backend Spring Boot. Дополнительные npm-пакеты не нужны; требуется Node.js 18 или новее.

## Запуск

### Backend и MySQL

Требуются JDK 25 и локальный MySQL 8. Spring Boot применяет Flyway-миграции из `src/main/resources/db/migration` и проверяет схему через Hibernate (`ddl-auto: validate`).

Задайте адрес и пользователя в PowerShell-сеансе. Пароль вводится скрыто и не записывается в `application.yml`, историю команд или файлы проекта:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/SmartPass?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USER = 'root'
$securePassword = Read-Host 'MySQL password' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    .\gradlew.bat bootRun
}
finally {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
```

Для другой локальной БД замените URL и пользователя. Обычный запуск Flyway применяет миграции и изменяет схему. Для уже подготовленной БД, которую нужно оставить без миграций, перед запуском добавьте `$env:SPRING_FLYWAY_ENABLED = 'false'`; Hibernate останется в режиме `validate`, поэтому схема должна уже соответствовать моделям приложения. Если схемы ещё нет, не отключайте Flyway и сначала согласуйте применение миграций.

Spring Security использует HTTP Basic. Для входа в веб-интерфейс нужна существующая активная учётная запись приложения с ролью `ADMIN` или `SECURITY`; пароль веб-учётной записи отличается от `DB_PASSWORD`. Миграция V12 добавляет хранение сообщений, собраний и редактируемых записей явки; фактические проходы в `AccessLogs` остаются отдельным аудитом.

### Первичная учётная запись ADMIN

Миграции создают роли и зоны доступа, но не создают пользователей. Если таблица `users` пуста, запустите **из интерактивного локального PowerShell в корне репозитория**:

```powershell
.\tools\bootstrap-admin.ps1 -ConfirmEmptyDatabase
```

Сценарий ограничен `localhost:3306/SmartPass`, требует явного флага и подтверждения `CREATE INITIAL ADMIN`, отказывает, если в `users` уже есть хотя бы одна запись, и атомарно создаёт только одного пользователя с ролью `ADMIN`. В терминале будут запрошены логин, профиль, зона и пароль приложения; пароль вводится скрыто дважды и сохраняется только как BCrypt-хеш. Значение `DB_PASSWORD` берётся из локального окружения, секрет не передаётся в аргументах, исходниках или файлах. Не запускайте bootstrap на другой или рабочей базе.

Проверить компиляцию и BCrypt/ограничение локального адреса без подключения к БД можно командой `.\tools\bootstrap-admin.ps1 -SelfTest`.

### Node.js и frontend

После успешного запуска backend в отдельном PowerShell-сеансе из каталога `server` выполните:

```powershell
npm start
```

Откройте <http://localhost:3000/> или <http://127.0.0.1:3000/>: оба локальных адреса разрешены в CORS по умолчанию, а корневой адрес отдаёт страницу `Frontend/pages/login.html`. Если задана `APP_CORS_ALLOWED_ORIGINS`, укажите в ней используемый адрес frontend. В интерфейсе подключены аутентификация и список пользователей, создание/редактирование пользователя, сообщения, календарь собраний и редактируемый журнал явки. Создавать пользователей и управлять собраниями может `ADMIN`; `SECURITY` имеет доступ в рамках правил API. Браузер хранит HTTP Basic credentials только в `sessionStorage` текущей вкладки и удаляет их при выходе. Не вводите пароль в URL и не сохраняйте его в файлах проекта. Восстановление пароля и ссылки Telegram/email/соцсетей отключены, пока не настроены сервис и адреса.

Node.js проксирует запросы `/api/*` в Spring backend, по умолчанию доступный на `http://127.0.0.1:8080`. Для проверки доступности только Node.js используйте `GET http://localhost:3000/api/health`, ответ — `{"status":"ok"}`.

Параметры можно переопределить переменными окружения:

| Переменная | По умолчанию | Назначение |
| --- | --- | --- |
| `HOST` | `127.0.0.1` | Интерфейс, на котором слушает локальный сервер |
| `PORT` | `3000` | Порт Node.js-сервера |
| `API_TARGET` | `http://127.0.0.1:8080` | HTTP(S)-адрес Spring backend |
| `FRONTEND_DIR` | `../Frontend` относительно каталога `server` | Каталог статических файлов |

Проверки сервера запускаются командой `npm test` из каталога `server`.
