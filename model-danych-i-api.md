# Model danych + specyfikacja API
## System zarządzania inteligentnym budynkiem – backend

> Dokument towarzyszący [`plan-pracy-backend.md`](./plan-pracy-backend.md). Zawiera **model danych** (encje, relacje) oraz **pełną specyfikację API** (REST + WebSocket).

> ⚠️ Schemat bazy generuje **Hibernate** z encji JPA (`ddl-auto=update`). Tabele i kolumny powstają automatycznie na podstawie adnotacji w klasach Java – nie ma plików SQL z migracjami.

---

## Spis treści

1. [Model danych – encje](#1-model-danych--encje)
2. [Diagram relacji (ERD)](#2-diagram-relacji-erd)
3. [Schemat bazy – referencja (co Hibernate wygeneruje)](#3-schemat-bazy--referencja-co-hibernate-wygeneruje)
4. [Specyfikacja REST API](#4-specyfikacja-rest-api)
5. [Specyfikacja WebSocket (STOMP)](#5-specyfikacja-websocket-stomp)
6. [Standardy odpowiedzi i błędów](#6-standardy-odpowiedzi-i-błędów)

---

## 1. Model danych – encje

### `User`
Reprezentuje konto użytkownika systemu.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | Auto-increment |
| `username` | `String` (unikalny) | Login |
| `email` | `String` (unikalny) | E-mail |
| `passwordHash` | `String` | BCrypt |
| `firstName` | `String` | |
| `lastName` | `String` | |
| `role` | `Role` (enum) | `ADMIN` / `BUILDING_MANAGER` / `RESIDENT` |
| `enabled` | `boolean` | Soft delete / dezaktywacja |
| `createdAt` | `Instant` | Auditing |
| `updatedAt` | `Instant` | Auditing |

### `Role` (enum)
- `ADMIN` – pełen dostęp do wszystkiego
- `BUILDING_MANAGER` – zarządca: zarządza pomieszczeniami, urządzeniami, harmonogramami, regułami; nie zarządza użytkownikami
- `RESIDENT` – mieszkaniec: przegląda i steruje urządzeniami; nie tworzy struktury

### `Room`
Pomieszczenie w budynku.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `name` | `String` (unikalny) | np. "Salon", "Biuro 2A" |
| `description` | `String` (nullable) | |
| `floor` | `int` | Piętro |
| `area` | `BigDecimal` (nullable) | Powierzchnia w m² |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | |

### `Device`
Urządzenie w pomieszczeniu (lampa, grzejnik, klimatyzacja, ...).

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `name` | `String` | |
| `type` | `DeviceType` (enum) | |
| `room` | `Room` (`@ManyToOne`, FK `room_id`) | |
| `status` | `DeviceStatus` (enum) | `ON` / `OFF` / `OFFLINE` / `ERROR` |
| `properties` | `String` (TEXT, JSON) | np. `{"brightness":80}` dla lampy |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | |

### `DeviceType` (enum)
`LIGHT, HEATER, AIR_CONDITIONER, FAN, BLINDS, LOCK, OUTLET, SPEAKER`

### `DeviceStatus` (enum)
`ON, OFF, OFFLINE, ERROR`

### `Sensor`
Czujnik (symulowany).

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `name` | `String` | |
| `type` | `SensorType` (enum) | |
| `room` | `Room` (`@ManyToOne`) | |
| `device` | `Device` (`@ManyToOne`, nullable) | jeśli czujnik wbudowany w urządzenie |
| `unit` | `String` | np. "°C", "%", "bool" |
| `enabled` | `boolean` | symulator pomija wyłączone |
| `createdAt` | `Instant` | |

### `SensorType` (enum)
`TEMPERATURE, HUMIDITY, MOTION, DOOR_WINDOW`

### `SensorReading`
Pojedynczy odczyt z czujnika. **Najliczniejsza tabela** – wymaga indeksu i ewentualnego TTL.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK, BIGINT) | |
| `sensor` | `Sensor` (`@ManyToOne`, FK `sensor_id`) | |
| `value` | `BigDecimal` (nullable) | dla typów numerycznych |
| `valueText` | `String` (nullable) | dla MOTION/DOOR_WINDOW: `DETECTED`/`OPEN`/`CLOSED` |
| `recordedAt` | `Instant` | |

> **Indeks kluczowy:** `(sensor_id, recorded_at DESC)` – używany przy historii i raportach. **Trzeba go zadeklarować jawnie** przez `@Table(indexes = @Index(...))` na encji, bo Hibernate domyślnie sam go nie utworzy.

### `AutomationRule`
Reguła automatyzacji.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `name` | `String` | |
| `enabled` | `boolean` | |
| `sensor` | `Sensor` (`@ManyToOne`) | źródło warunku |
| `operator` | `Operator` (enum) | `LT, LTE, GT, GTE, EQ, NEQ` |
| `threshold` | `BigDecimal` | wartość progowa |
| `targetDevice` | `Device` (`@ManyToOne`) | urządzenie do akcji |
| `actionType` | `ActionType` (enum) | `TURN_ON, TURN_OFF, SET_PROPERTY` |
| `actionPayload` | `String` (JSON, nullable) | np. `{"brightness":50}` |
| `createdBy` | `User` (`@ManyToOne`) | |
| `createdAt` | `Instant` | |

### `Schedule`
Harmonogram CRON dla urządzenia.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `name` | `String` | |
| `device` | `Device` (`@ManyToOne`) | |
| `cronExpression` | `String` | np. `0 0 22 * * *` (codziennie 22:00) |
| `actionType` | `ActionType` | |
| `actionPayload` | `String` (JSON, nullable) | |
| `enabled` | `boolean` | |
| `createdAt` | `Instant` | |

### `Alert`
Alert / powiadomienie systemowe.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK) | |
| `type` | `AlertType` (enum) | |
| `severity` | `AlertSeverity` (enum) | `INFO, WARNING, CRITICAL` |
| `message` | `String` | |
| `sourceType` | `String` | `SENSOR`, `DEVICE`, `SYSTEM` |
| `sourceId` | `Long` (nullable) | |
| `acknowledged` | `boolean` | |
| `acknowledgedBy` | `User` (`@ManyToOne`, nullable) | |
| `acknowledgedAt` | `Instant` (nullable) | |
| `createdAt` | `Instant` | |

### `AlertType` (enum)
`THRESHOLD_EXCEEDED, DEVICE_OFFLINE, MOTION_DETECTED_NIGHT, DOOR_OPEN_TOO_LONG, SYSTEM_ERROR`

### `EventLog`
Wpis w historii zdarzeń.

| Pole | Typ | Opis |
|---|---|---|
| `id` | `Long` (PK, BIGINT) | |
| `eventType` | `EventType` (enum) | |
| `user` | `User` (`@ManyToOne`, nullable) | null dla zdarzeń systemowych |
| `entityType` | `String` (nullable) | `ROOM`, `DEVICE`, `RULE`, ... |
| `entityId` | `Long` (nullable) | |
| `payload` | `String` (TEXT, JSON, nullable) | szczegóły |
| `occurredAt` | `Instant` | |

> **Indeksy:** `(event_type, occurred_at DESC)` i `(user_id, occurred_at DESC)` – jawnie przez `@Table(indexes=...)`.

---

## 2. Diagram relacji (ERD)

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│   User   │────────<│ EventLog │         │   Role   │
└──────────┘  user_id└──────────┘         │  (enum)  │
     │                                     └──────────┘
     │ acknowledged_by
     ▼
┌──────────┐         ┌──────────┐         ┌──────────┐
│  Alert   │         │   Room   │────────<│  Device  │
└──────────┘         └──────────┘  room_id└──────────┘
                          │                    │
                          │                    │
                          │1:N                 │1:N (opcjonalnie)
                          ▼                    │
                     ┌──────────┐               │
                     │  Sensor  │<──────────────┘
                     └──────────┘  device_id (nullable)
                          │
                          │1:N
                          ▼
                  ┌────────────────┐
                  │ SensorReading  │
                  └────────────────┘

┌────────────────┐       ┌──────────┐
│ AutomationRule │──────>│  Sensor  │ (sensor_id)
│                │──────>│  Device  │ (target_device_id)
└────────────────┘       └──────────┘

┌──────────┐
│ Schedule │──────>│  Device  │ (device_id)
└──────────┘
```

---

## 3. Schemat bazy – referencja (co Hibernate wygeneruje)

> Tabele tworzy automatycznie Hibernate przy starcie aplikacji (na podstawie adnotacji JPA na encjach). Poniższe DDL pokazuje **przybliżony, oczekiwany kształt** – służy jako referencja do code review i debugowania (np. żeby sprawdzić, czy faktycznie powstał oczekiwany indeks).

> **Praktyczne wskazówki dla encji:**
> - `@Table(name="users")` na encji `User` – inaczej tabela nazwie się `user` (zarezerwowane słowo w niektórych dialektach)
> - `@Enumerated(EnumType.STRING)` dla każdego enum-a (zapis tekstowy, nie ordinalny)
> - `@Column(unique=true)` lub `@Table(uniqueConstraints=...)` dla `username`, `email`, `Room.name`
> - `@Column(precision=10, scale=3)` dla `BigDecimal` (`value`, `threshold`)
> - `@Column(columnDefinition="TEXT")` dla pól JSON (`properties`, `actionPayload`, `payload`)
> - `@Table(indexes={@Index(name="...", columnList="...")})` dla wydajnościowych indeksów (najważniejsze: `sensor_readings(sensor_id, recorded_at DESC)`)
> - `@CreatedDate` / `@LastModifiedDate` z `@EntityListeners(AuditingEntityListener.class)` dla `createdAt`/`updatedAt`

```sql
-- Tabela: users
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    role          VARCHAR(255) NOT NULL,
    enabled       BIT(1)       NOT NULL,
    created_at    DATETIME(6),
    updated_at    DATETIME(6)
);

-- Tabela: rooms
CREATE TABLE rooms (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    floor       INT,
    area        DECIMAL(10,2),
    created_at  DATETIME(6),
    updated_at  DATETIME(6)
);

-- Tabela: devices
CREATE TABLE devices (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    room_id    BIGINT       NOT NULL,
    status     VARCHAR(255) NOT NULL,
    properties TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_device_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Tabela: sensors
CREATE TABLE sensors (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    room_id    BIGINT       NOT NULL,
    device_id  BIGINT,
    unit       VARCHAR(255),
    enabled    BIT(1)       NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_sensor_room   FOREIGN KEY (room_id)   REFERENCES rooms(id),
    CONSTRAINT fk_sensor_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

-- Tabela: sensor_readings (najgrubsza – z indeksem!)
CREATE TABLE sensor_readings (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id   BIGINT       NOT NULL,
    value       DECIMAL(10,3),
    value_text  VARCHAR(255),
    recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_reading_sensor FOREIGN KEY (sensor_id) REFERENCES sensors(id),
    INDEX idx_readings_sensor_time (sensor_id, recorded_at)
);

-- Tabela: automation_rules
CREATE TABLE automation_rules (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    enabled          BIT(1)       NOT NULL,
    sensor_id        BIGINT       NOT NULL,
    operator         VARCHAR(255) NOT NULL,
    threshold        DECIMAL(10,3) NOT NULL,
    target_device_id BIGINT       NOT NULL,
    action_type      VARCHAR(255) NOT NULL,
    action_payload   TEXT,
    created_by       BIGINT       NOT NULL,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    CONSTRAINT fk_rule_sensor FOREIGN KEY (sensor_id)        REFERENCES sensors(id),
    CONSTRAINT fk_rule_device FOREIGN KEY (target_device_id) REFERENCES devices(id),
    CONSTRAINT fk_rule_user   FOREIGN KEY (created_by)       REFERENCES users(id)
);

-- Tabela: schedules
CREATE TABLE schedules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    device_id       BIGINT       NOT NULL,
    cron_expression VARCHAR(255) NOT NULL,
    action_type     VARCHAR(255) NOT NULL,
    action_payload  TEXT,
    enabled         BIT(1)       NOT NULL,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT fk_schedule_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

-- Tabela: alerts
CREATE TABLE alerts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    type             VARCHAR(255) NOT NULL,
    severity         VARCHAR(255) NOT NULL,
    message          VARCHAR(512) NOT NULL,
    source_type      VARCHAR(255) NOT NULL,
    source_id        BIGINT,
    acknowledged     BIT(1)       NOT NULL,
    acknowledged_by  BIGINT,
    acknowledged_at  DATETIME(6),
    created_at       DATETIME(6),
    CONSTRAINT fk_alert_user FOREIGN KEY (acknowledged_by) REFERENCES users(id),
    INDEX idx_alerts_ack_created (acknowledged, created_at)
);

-- Tabela: event_log
CREATE TABLE event_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(255) NOT NULL,
    user_id     BIGINT,
    entity_type VARCHAR(255),
    entity_id   BIGINT,
    payload     TEXT,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_event_type_time (event_type, occurred_at),
    INDEX idx_event_user_time (user_id, occurred_at)
);
```

> **Po starcie aplikacji można sprawdzić wygenerowany schemat:**
> `mariadb -u app -p smart_building -e "SHOW CREATE TABLE sensor_readings;"`
> Jeśli czegoś brakuje (np. indeksu), znaczy że trzeba poprawić adnotacje na encji.

---

## 4. Specyfikacja REST API

> Wszystkie endpointy poza `auth` wymagają nagłówka `Authorization: Bearer <jwt>`.
> Format dat: ISO-8601 (`2026-05-06T14:30:00Z`).
> Paginacja: parametry `page` (od 0) i `size` (domyślnie 20, max 100).

### 4.1. Auth – `/api/auth`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `POST` | `/api/auth/register` | publiczne | Rejestracja (domyślnie `RESIDENT`) |
| `POST` | `/api/auth/login` | publiczne | Logowanie → zwraca JWT |
| `POST` | `/api/auth/logout` | dowolna | (opcjonalnie – stateless, no-op po stronie serwera) |
| `GET`  | `/api/auth/me` | dowolna | Profil zalogowanego usera |

### 4.2. Użytkownicy – `/api/users`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/users` | ADMIN | Lista wszystkich (paginacja) |
| `GET` | `/api/users/{id}` | ADMIN / własny profil | Pojedynczy user |
| `PATCH` | `/api/users/{id}` | ADMIN / własny profil | Update danych (firstName, lastName, email) |
| `PATCH` | `/api/users/{id}/role` | ADMIN | Zmiana roli |
| `PATCH` | `/api/users/{id}/password` | własny | Zmiana własnego hasła |
| `DELETE` | `/api/users/{id}` | ADMIN | Soft delete (`enabled=false`) |

### 4.3. Pomieszczenia – `/api/rooms`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/rooms` | dowolna | Lista pomieszczeń |
| `GET` | `/api/rooms/{id}` | dowolna | Szczegóły |
| `POST` | `/api/rooms` | ADMIN, BUILDING_MANAGER | Nowe pomieszczenie |
| `PUT` | `/api/rooms/{id}` | ADMIN, BUILDING_MANAGER | Edycja |
| `DELETE` | `/api/rooms/{id}` | ADMIN | Usunięcie |

### 4.4. Urządzenia – `/api/devices`, `/api/rooms/{id}/devices`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/devices?roomId=&type=&status=` | dowolna | Lista z filtrami |
| `GET` | `/api/rooms/{roomId}/devices` | dowolna | Urządzenia w pokoju |
| `GET` | `/api/devices/{id}` | dowolna | Szczegóły |
| `POST` | `/api/rooms/{roomId}/devices` | ADMIN, BUILDING_MANAGER | Dodaj urządzenie |
| `PUT` | `/api/devices/{id}` | ADMIN, BUILDING_MANAGER | Edycja |
| `DELETE` | `/api/devices/{id}` | ADMIN | Usunięcie |
| `POST` | `/api/devices/{id}/state` | dowolna | Zmiana stanu (`{"status":"ON"}` + opcjonalnie payload) |

### 4.5. Czujniki – `/api/sensors`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/sensors?roomId=&type=` | dowolna | Lista |
| `GET` | `/api/rooms/{roomId}/sensors` | dowolna | Czujniki w pokoju |
| `GET` | `/api/sensors/{id}` | dowolna | Szczegóły |
| `POST` | `/api/sensors` | ADMIN, BUILDING_MANAGER | Nowy czujnik |
| `PATCH` | `/api/sensors/{id}` | ADMIN, BUILDING_MANAGER | Włącz/wyłącz, zmień nazwę |
| `DELETE` | `/api/sensors/{id}` | ADMIN | Usunięcie |
| `GET` | `/api/sensors/{id}/readings?from=&to=&page=&size=` | dowolna | Historia odczytów |
| `GET` | `/api/sensors/{id}/readings/latest` | dowolna | Ostatni odczyt |
| `GET` | `/api/sensors/{id}/stats?from=&to=` | dowolna | min/max/avg/count |

### 4.6. Reguły automatyzacji – `/api/automation/rules`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/automation/rules` | ADMIN, BUILDING_MANAGER | Lista |
| `GET` | `/api/automation/rules/{id}` | ADMIN, BUILDING_MANAGER | Szczegóły |
| `POST` | `/api/automation/rules` | ADMIN, BUILDING_MANAGER | Nowa reguła |
| `PUT` | `/api/automation/rules/{id}` | ADMIN, BUILDING_MANAGER | Edycja |
| `POST` | `/api/automation/rules/{id}/toggle` | ADMIN, BUILDING_MANAGER | Włącz/wyłącz |
| `DELETE` | `/api/automation/rules/{id}` | ADMIN | Usunięcie |

### 4.7. Harmonogramy – `/api/schedules`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/schedules?deviceId=` | ADMIN, BUILDING_MANAGER | Lista |
| `GET` | `/api/schedules/{id}` | ADMIN, BUILDING_MANAGER | Szczegóły |
| `POST` | `/api/schedules` | ADMIN, BUILDING_MANAGER | Nowy |
| `PUT` | `/api/schedules/{id}` | ADMIN, BUILDING_MANAGER | Edycja |
| `POST` | `/api/schedules/{id}/toggle` | ADMIN, BUILDING_MANAGER | Włącz/wyłącz |
| `DELETE` | `/api/schedules/{id}` | ADMIN | Usunięcie |

### 4.8. Alerty – `/api/alerts`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/alerts?acknowledged=&severity=` | dowolna | Lista |
| `GET` | `/api/alerts/{id}` | dowolna | Szczegóły |
| `POST` | `/api/alerts/{id}/acknowledge` | dowolna | Kwitowanie |
| `DELETE` | `/api/alerts/{id}` | ADMIN | Usunięcie |

### 4.9. Historia zdarzeń – `/api/events`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/events?type=&userId=&entityType=&from=&to=&page=&size=` | ADMIN, BUILDING_MANAGER | Lista wpisów |

### 4.10. Raporty – `/api/reports`

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| `GET` | `/api/reports/sensor-summary?sensorId=&from=&to=` | dowolna | min/max/avg/count |
| `GET` | `/api/reports/device-activity?deviceId=&from=&to=` | dowolna | Liczba przełączeń, czas ON/OFF |
| `GET` | `/api/reports/system-health` | ADMIN, BUILDING_MANAGER | Aktualne statystyki systemu |

### 4.11. Przykładowe payloady

**Login request:**
```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**Login response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-05-06T15:30:00Z",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN"
  }
}
```

**Create rule request:**
```json
POST /api/automation/rules
{
  "name": "Włącz grzejnik gdy zimno",
  "sensorId": 5,
  "operator": "LT",
  "threshold": 18.0,
  "targetDeviceId": 12,
  "actionType": "TURN_ON",
  "actionPayload": null
}
```

**Create schedule request:**
```json
POST /api/schedules
{
  "name": "Zgaś światło na noc",
  "deviceId": 7,
  "cronExpression": "0 0 23 * * *",
  "actionType": "TURN_OFF",
  "enabled": true
}
```

---

## 5. Specyfikacja WebSocket (STOMP)

### Endpoint połączenia

```
ws://localhost:8080/ws
```

Klient łączy się z tokenem JWT (header `Authorization: Bearer <token>` w `connectHeaders` STOMP).

### Topiki (broadcast – `/topic`)

| Topic | Payload | Kiedy emitowany |
|---|---|---|
| `/topic/sensors/{sensorId}/readings` | `{ sensorId, value, valueText, recordedAt, unit }` | Po każdym nowym odczycie symulatora |
| `/topic/devices/{deviceId}/state` | `{ deviceId, status, properties, changedAt, changedBy }` | Po zmianie stanu urządzenia |
| `/topic/alerts` | `{ id, type, severity, message, sourceType, sourceId, createdAt }` | Po wygenerowaniu alertu |
| `/topic/rooms/{roomId}/summary` | (opcjonalnie w Sprincie 6) | Agregat per pokój co 30s |

### Kolejki użytkownika (`/user/queue`)

| Kolejka | Payload | Kiedy |
|---|---|---|
| `/user/queue/notifications` | `{ message, type, createdAt }` | Powiadomienia targetowane (np. przypisany zarządca dostaje info o krytycznym alercie) |

### Przykład klienta JS

```javascript
const stomp = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: `Bearer ${token}` }
});
stomp.onConnect = () => {
  stomp.subscribe('/topic/sensors/5/readings', msg => {
    console.log(JSON.parse(msg.body));
  });
  stomp.subscribe('/topic/alerts', msg => { /* ... */ });
};
stomp.activate();
```

---

## 6. Standardy odpowiedzi i błędów

### Sukces

- `200 OK` – dla GET, PUT, PATCH zwracających ciało
- `201 Created` – dla POST tworzących zasób; w nagłówku `Location: /api/.../{id}`
- `204 No Content` – dla DELETE i operacji bez ciała odpowiedzi

### Błędy – format `ApiError`

```json
{
  "timestamp": "2026-05-06T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Pole 'email' jest wymagane",
  "path": "/api/auth/register",
  "errors": [
    { "field": "email", "message": "must not be blank" },
    { "field": "password", "message": "size must be at least 8" }
  ]
}
```

### Kody HTTP

| Kod | Kiedy |
|---|---|
| `400` | Walidacja wejścia, niepoprawny CRON, nieznany operator |
| `401` | Brak tokenu, token wygasły, niepoprawny token |
| `403` | Brak uprawnień (zła rola) |
| `404` | Zasób nieznaleziony |
| `409` | Konflikt (duplikat username/email/nazwy pokoju) |
| `422` | Logika domenowa (np. próba ustawienia niedozwolonej zmiany stanu) |
| `500` | Niespodziewany błąd serwera |

### Custom error codes (przykłady)

- `VALIDATION_FAILED`
- `INVALID_CREDENTIALS`
- `USERNAME_ALREADY_EXISTS`
- `EMAIL_ALREADY_EXISTS`
- `RESOURCE_NOT_FOUND`
- `INVALID_DEVICE_STATE_TRANSITION`
- `INVALID_CRON_EXPRESSION`
- `RULE_DEBOUNCED`

---

**Powiązane dokumenty:**
- [`plan-pracy-backend.md`](./plan-pracy-backend.md) – plan pracy, struktura projektu, sprinty, checklisty
