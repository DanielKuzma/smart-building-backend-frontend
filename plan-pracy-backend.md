# Plan pracy – Backend
## System zarządzania inteligentnym budynkiem

> **Autor części backendowej:** Andrzej Raczkowski
> **Projekt grupowy:** UTH im. H. Chodkowskiej, Warszawa 2026
> **Dokument towarzyszący:** [`model-danych-i-api.md`](./model-danych-i-api.md)

---

## Spis treści

1. [Cel i kontekst dokumentu](#1-cel-i-kontekst-dokumentu)
2. [Stack technologiczny](#2-stack-technologiczny)
3. [Architektura wysokopoziomowa](#3-architektura-wysokopoziomowa)
4. [Struktura projektu (drzewo katalogów)](#4-struktura-projektu-drzewo-katalogów)
5. [Konfiguracja środowiska](#5-konfiguracja-środowiska)
6. [Konwencje pracy](#6-konwencje-pracy)
7. [Plan sprintów – szczegółowa checklista](#7-plan-sprintów--szczegółowa-checklista)
   - [Sprint 0 – Setup projektu (2–3 dni)](#sprint-0--setup-projektu-23-dni)
   - [Sprint 1 – Auth + użytkownicy + role (1 tydzień)](#sprint-1--auth--użytkownicy--role-1-tydzień)
   - [Sprint 2 – Pomieszczenia + urządzenia (1 tydzień)](#sprint-2--pomieszczenia--urządzenia-1-tydzień)
   - [Sprint 3 – Czujniki + symulator + WebSocket (1–1,5 tygodnia)](#sprint-3--czujniki--symulator--websocket-115-tygodnia)
   - [Sprint 4 – Reguły automatyzacji + harmonogramy (1 tydzień)](#sprint-4--reguły-automatyzacji--harmonogramy-1-tydzień)
   - [Sprint 5 – Alerty + historia zdarzeń + raporty (1 tydzień)](#sprint-5--alerty--historia-zdarzeń--raporty-1-tydzień)
   - [Sprint 6 – Testy E2E + dokumentacja + finalne porządki (1 tydzień)](#sprint-6--testy-e2e--dokumentacja--finalne-porządki-1-tydzień)
8. [Definition of Done](#8-definition-of-done)
9. [Strategia testowania](#9-strategia-testowania)
10. [Ryzyka i punkty uwagi](#10-ryzyka-i-punkty-uwagi)

---

## 1. Cel i kontekst dokumentu

Dokument opisuje **plan pracy nad backendem** systemu zarządzania inteligentnym budynkiem. Backend ma realizować wszystkie wymagania funkcjonalne z dokumentu projektowego: zarządzanie użytkownikami / pomieszczeniami / urządzeniami, odczyt danych z (symulowanych) czujników, automatyzację, harmonogramy, alerty, historię zdarzeń i raportowanie.

Frontendem zajmuje się Daniel – backend wystawia REST API + kanały WebSocket (STOMP) jako jedyny punkt integracji. Modele danych i pełna lista endpointów są w osobnym pliku.

---

## 2. Stack technologiczny

| Warstwa | Technologia | Uzasadnienie |
|---|---|---|
| Język | Java 21 LTS | Wymagane przez Spring Boot 4 (min. Java 17, LTS = bezpieczna decyzja) |
| Framework | Spring Boot 4.x | Wybór z założenia projektu |
| Web | Spring Web MVC | Standard dla REST w Spring |
| Real-time | Spring WebSocket + STOMP | De facto standard – natywne wsparcie, łatwe broadcasty per-topic |
| Persystencja | Spring Data JPA + Hibernate | Mniej boilerplate, łatwe repozytoria |
| Baza danych (dev/prod) | MariaDB 11.x | Wybór z założenia projektu, instalacja lokalna |
| Schemat bazy | Hibernate `ddl-auto=update` | Zero migracji – tabele generowane z encji |
| Baza danych (test) | H2 in-memory (tryb MariaDB) | Zero setupu, świeża baza per uruchomienie testów |
| Bezpieczeństwo | Spring Security + JWT | Standard branżowy, stateless, dobrze gra z SPA |
| Walidacja | Jakarta Bean Validation | Wbudowane, deklaratywne |
| Mapowanie DTO | MapStruct | Generuje mappery w czasie kompilacji – zero refleksji |
| Boilerplate | Lombok | `@Getter`, `@Builder`, `@Slf4j` itd. |
| Seed data | `CommandLineRunner` + `@Profile("dev")` | Java zamiast SQL – elastyczne, łatwe do rozbudowy |
| Harmonogramy | Spring `@Scheduled` + Quartz | Quartz dla harmonogramów per-urządzenie zapisanych w DB |
| Dokumentacja API | springdoc-openapi | Generuje Swagger UI na podstawie kodu |
| Testy jednostkowe | JUnit 5 + Mockito + AssertJ | Standard |
| Testy integracyjne | Spring Boot Test + H2 | Bez Dockera, baza efemeryczna w pamięci |
| Testy E2E (API) | RestAssured + H2 | Czytelne assercje na całym cyklu HTTP |
| Build | Maven | Sprawdzony w ekosystemie Spring |

**Wersje konkretne** (do ustalenia w `pom.xml` w Sprincie 0): Spring Boot 4.x, Java 21, MariaDB 11, H2 2.2+, JUnit 5.10+, springdoc-openapi 2.6+.

> ⚠️ **Uwaga o `ddl-auto=update`:** Hibernate dodaje nowe kolumny i tabele, ale **nie usuwa** ani nie modyfikuje istniejących. W razie poważnych zmian schematu w trakcie devu – usuń bazę i odpal aplikację ponownie (seed zrobi resztę). W przyszłości można dołożyć Flyway, jeśli pojawi się potrzeba.

---

## 3. Architektura wysokopoziomowa

```
                    ┌─────────────────────┐
                    │   Frontend (SPA)    │
                    └──────────┬──────────┘
                               │ REST + WebSocket/STOMP
                               │ JWT w Authorization header
                  ┌────────────▼─────────────┐
                  │     Spring Boot 4.x      │
                  │ ┌──────────────────────┐ │
                  │ │  Controllers (REST)  │ │
                  │ │  WebSocket Endpoints │ │
                  │ ├──────────────────────┤ │
                  │ │   Service Layer      │ │
                  │ │  - business logic    │ │
                  │ │  - rule engine       │ │
                  │ │  - alert dispatcher  │ │
                  │ ├──────────────────────┤ │
                  │ │   Repository (JPA)   │ │
                  │ ├──────────────────────┤ │
                  │ │   Symulator          │ │
                  │ │   (@Scheduled task)  │ │
                  │ ├──────────────────────┤ │
                  │ │   DataSeeder (dev)   │ │
                  │ └──────────┬───────────┘ │
                  └────────────┼─────────────┘
                               │ JDBC
                       ┌───────▼────────┐
                       │   MariaDB 11   │   ← lokalnie zainstalowana
                       └────────────────┘
```

**Przepływ symulacji + real-time:**

1. Komponent `SensorSimulator` (Spring `@Scheduled`, np. co 5 s) generuje losowe odczyty dla każdego aktywnego czujnika.
2. Odczyty zapisywane są do tabeli `sensor_readings` (asynchronicznie / batch).
3. `AutomationEngine` ocenia reguły – jeśli warunek spełniony, zmienia stan urządzenia + tworzy wpis w `event_log`.
4. `WebSocketBroadcaster` publikuje aktualizację na właściwy topic STOMP (`/topic/sensors/{id}/readings`, `/topic/devices/{id}/state`).
5. Frontend subskrybujący topic dostaje update w czasie rzeczywistym.

**Moduły (logiczne):** `auth`, `user`, `room`, `device`, `sensor`, `automation`, `schedule`, `alert`, `eventlog`, `report`, `simulator`, `websocket`, `seed`, `common`.

---

## 4. Struktura projektu (drzewo katalogów)

```
smart-building-backend/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   ├── plan-pracy-backend.md          ← ten plik
│   └── model-danych-i-api.md          ← model + endpointy
└── src/
    ├── main/
    │   ├── java/pl/uth/smartbuilding/
    │   │   ├── SmartBuildingApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── WebSocketConfig.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   ├── JpaAuditingConfig.java
    │   │   │   ├── SchedulingConfig.java
    │   │   │   └── CorsConfig.java
    │   │   ├── auth/
    │   │   │   ├── controller/AuthController.java
    │   │   │   ├── service/AuthService.java
    │   │   │   ├── dto/{LoginRequest,RegisterRequest,JwtResponse}.java
    │   │   │   ├── jwt/{JwtService,JwtAuthFilter}.java
    │   │   │   └── exception/InvalidCredentialsException.java
    │   │   ├── user/
    │   │   │   ├── controller/UserController.java
    │   │   │   ├── service/UserService.java
    │   │   │   ├── repository/UserRepository.java
    │   │   │   ├── domain/{User,Role}.java
    │   │   │   ├── dto/{UserResponse,UpdateUserRequest,ChangeRoleRequest}.java
    │   │   │   └── mapper/UserMapper.java
    │   │   ├── room/
    │   │   │   ├── controller/RoomController.java
    │   │   │   ├── service/RoomService.java
    │   │   │   ├── repository/RoomRepository.java
    │   │   │   ├── domain/Room.java
    │   │   │   ├── dto/{RoomRequest,RoomResponse}.java
    │   │   │   └── mapper/RoomMapper.java
    │   │   ├── device/
    │   │   │   ├── controller/DeviceController.java
    │   │   │   ├── service/{DeviceService,DeviceStateService}.java
    │   │   │   ├── repository/DeviceRepository.java
    │   │   │   ├── domain/{Device,DeviceType,DeviceStatus}.java
    │   │   │   ├── dto/{DeviceRequest,DeviceResponse,ChangeStateRequest}.java
    │   │   │   └── mapper/DeviceMapper.java
    │   │   ├── sensor/
    │   │   │   ├── controller/SensorController.java
    │   │   │   ├── service/{SensorService,SensorReadingService}.java
    │   │   │   ├── repository/{SensorRepository,SensorReadingRepository}.java
    │   │   │   ├── domain/{Sensor,SensorType,SensorReading}.java
    │   │   │   ├── dto/{SensorRequest,SensorResponse,ReadingResponse}.java
    │   │   │   └── mapper/SensorMapper.java
    │   │   ├── automation/
    │   │   │   ├── controller/AutomationController.java
    │   │   │   ├── service/{AutomationService,RuleEngine,RuleEvaluator}.java
    │   │   │   ├── repository/AutomationRuleRepository.java
    │   │   │   ├── domain/{AutomationRule,RuleCondition,RuleAction}.java
    │   │   │   └── dto/{RuleRequest,RuleResponse}.java
    │   │   ├── schedule/
    │   │   │   ├── controller/ScheduleController.java
    │   │   │   ├── service/{ScheduleService,ScheduleExecutor}.java
    │   │   │   ├── repository/ScheduleRepository.java
    │   │   │   ├── domain/Schedule.java
    │   │   │   └── dto/{ScheduleRequest,ScheduleResponse}.java
    │   │   ├── alert/
    │   │   │   ├── controller/AlertController.java
    │   │   │   ├── service/{AlertService,AlertDispatcher}.java
    │   │   │   ├── repository/AlertRepository.java
    │   │   │   ├── domain/{Alert,AlertSeverity,AlertType}.java
    │   │   │   └── dto/AlertResponse.java
    │   │   ├── eventlog/
    │   │   │   ├── controller/EventLogController.java
    │   │   │   ├── service/EventLogService.java
    │   │   │   ├── repository/EventLogRepository.java
    │   │   │   ├── domain/{EventLog,EventType}.java
    │   │   │   └── dto/EventLogResponse.java
    │   │   ├── report/
    │   │   │   ├── controller/ReportController.java
    │   │   │   ├── service/ReportService.java
    │   │   │   └── dto/{SensorSummary,DeviceActivity,SystemHealth}.java
    │   │   ├── simulator/
    │   │   │   ├── SensorSimulator.java   ← @Scheduled, generuje losowe dane
    │   │   │   └── ReadingGenerator.java  ← logika per typ czujnika
    │   │   ├── seed/
    │   │   │   └── DataSeeder.java        ← CommandLineRunner, @Profile("dev")
    │   │   ├── websocket/
    │   │   │   ├── WebSocketBroadcaster.java
    │   │   │   └── topics/Topics.java     ← stałe nazwy topiców
    │   │   └── common/
    │   │       ├── exception/{GlobalExceptionHandler,ApiError,NotFoundException}.java
    │   │       ├── audit/AuditingEntity.java   ← @MappedSuperclass createdAt/updatedAt
    │   │       └── util/{PageRequestFactory,DateRange}.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── logback-spring.xml
    └── test/
        ├── java/pl/uth/smartbuilding/
        │   ├── unit/         ← testy serwisów z @Mock
        │   ├── integration/  ← @SpringBootTest + H2
        │   └── e2e/          ← RestAssured + pełen kontekst + H2
        └── resources/
            └── application-test.yml
```

---

## 5. Konfiguracja środowiska

### Wymagania lokalne

- JDK 21 (Temurin / Liberica)
- Maven 3.9+
- **MariaDB 11** zainstalowana lokalnie (Windows: instalator z mariadb.org; Linux: `apt install mariadb-server`; macOS: `brew install mariadb`)
- Git
- IDE: IntelliJ IDEA (Community wystarczy)

### Pierwsze uruchomienie MariaDB

Po zainstalowaniu MariaDB, jednorazowo:

```sql
-- zaloguj się jako root: mariadb -u root -p
CREATE DATABASE smart_building CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'app'@'localhost' IDENTIFIED BY 'app';
GRANT ALL PRIVILEGES ON smart_building.* TO 'app'@'localhost';
FLUSH PRIVILEGES;
```

Komendy te trafiają do `README.md` w sekcji Quick Start.

### `application.yml` – fragmenty kluczowe

```yaml
# application.yml (wspólne)
spring:
  application:
    name: smart-building-backend
  jpa:
    open-in-view: false
    properties:
      hibernate.format_sql: true
server:
  port: 8080
springdoc:
  swagger-ui.path: /swagger-ui.html

# application-dev.yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/smart_building
    username: app
    password: app
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate.ddl-auto: update
    show-sql: true
    properties:
      hibernate.dialect: org.hibernate.dialect.MariaDBDialect
app:
  simulator:
    enabled: true
    interval-ms: 5000
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-me-in-prod-minimum-32-chars}
    ttl-minutes: 60

# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  jpa:
    hibernate.ddl-auto: create-drop
    properties:
      hibernate.dialect: org.hibernate.dialect.H2Dialect
app:
  simulator:
    enabled: false   # symulator wyłączony w testach
  jwt:
    secret: test-secret-test-secret-test-secret-test-secret
    ttl-minutes: 60
```

### Profile Spring

- `dev` – lokalna MariaDB, pełne logi SQL, CORS otwarty na `localhost:5173` (Vite), `DataSeeder` aktywny
- `test` – H2 in-memory w trybie kompatybilności z MariaDB, log na poziomie WARN, symulator wyłączony, brak seederu
- `prod` – konfiguracja z env-vars, logi JSON, CORS po whitelist, seeder wyłączony

---

## 6. Konwencje pracy

- **Branching:** `main` (zawsze działa) + `feature/<sprint-numer>-<nazwa>` (np. `feature/1-jwt-auth`).
- **Commity:** Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`).
- **PR/merge:** każdy feature trafia na `main` przez squash merge po zielonych testach.
- **Tagi:** `sprint-0-done`, `sprint-1-done`, ... – łatwo wrócić do stanu po sprincie.
- **Code style:** Google Java Style (plugin do IntelliJ); wymuszone przez Spotless w Maven.
- **Issue tracker:** GitHub Issues + tablica Projects (kolumny: Backlog → Sprint → In progress → Review → Done).

---

## 7. Plan sprintów – szczegółowa checklista

> Każdy task to zaznaczalny checkbox. Cel: po każdym sprincie aplikacja **kompiluje się, testy są zielone i da się ją uruchomić**.

### Sprint 0 – Setup projektu (2–3 dni)

**Cel:** "Hello world" w Spring Boot, połączenie z lokalną MariaDB, działający `ddl-auto`, H2 w testach, podstawowy CI.

#### Inicjalizacja repo i projektu
- [ ] Utwórz repozytorium GitHub `smart-building-backend`
- [ ] Wygeneruj projekt na `start.spring.io` (Maven, Java 21, Spring Boot 4.x)
  - Zależności: Web, Security, Data JPA, Validation, WebSocket, Lombok, MariaDB Driver, springdoc-openapi
- [ ] Dodaj ręcznie do `pom.xml`: H2 (`<scope>test</scope>`), MapStruct + processor, jjwt (api/impl/jackson)
- [ ] Skonfiguruj `.gitignore` (target/, .idea/, *.iml, logs/)
- [ ] Dodaj `.editorconfig` (UTF-8, LF, 4 spacje)
- [ ] Utwórz `README.md` z sekcjami: Wymagania, Instalacja MariaDB, Quick Start, Testy, Struktura, Domyślne konta
- [ ] Skonfiguruj Spotless w `pom.xml` (Google Java Style)
- [ ] Pierwszy commit: `chore: initial project skeleton`

#### Lokalna MariaDB
- [ ] Zainstaluj MariaDB 11 lokalnie
- [ ] Stwórz bazę `smart_building` + użytkownika `app/app` (komendy SQL z sekcji 5)
- [ ] Sprawdź połączenie: `mariadb -u app -p smart_building` → `SHOW TABLES;` (puste – ok)
- [ ] Dodaj te kroki do `README.md`

#### Konfiguracja Spring
- [ ] `application.yml` (wspólne ustawienia)
- [ ] `application-dev.yml` (datasource MariaDB, ddl-auto=update, show-sql, dialect)
- [ ] `application-test.yml` (datasource H2, ddl-auto=create-drop, dialect H2)
- [ ] `application-prod.yml` (placeholdery env-vars)
- [ ] Klasa `SmartBuildingApplication` z `@SpringBootApplication`
- [ ] Wyłącz domyślnego user/password Springa (na razie `permitAll`)
- [ ] Skonfiguruj `logback-spring.xml` (poziom INFO, format z timestampem)

#### Pierwsza tabela testowa (sanity check ddl-auto)
- [ ] Stwórz prostą encję `AppHealth` (id, ts) w pakiecie `common.health`
- [ ] Uruchom aplikację z profilem `dev` – Hibernate stworzy tabelę `app_health` w MariaDB
- [ ] Potwierdź w bazie: `SHOW TABLES;` → widać `app_health`
- [ ] Po sanity check encję można usunąć albo zostawić jako zabezpieczenie

#### Health check + dokumentacja
- [ ] Włącz Spring Boot Actuator (zależność + `management.endpoints.web.exposure.include=health,info`)
- [ ] Test ręczny: `curl localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] Skonfiguruj springdoc – `/swagger-ui.html` ładuje się
- [ ] Klasa `OpenApiConfig` z `@OpenAPIDefinition` (tytuł, wersja, opis)

#### Testy + CI
- [ ] Test `SmartBuildingApplicationTests` z `@ActiveProfiles("test")` (kontekst Springa się ładuje na H2) – zielony
- [ ] Klasa bazowa `IntegrationTestBase` – `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional` (rollback po teście)
- [ ] Test integracyjny `AppHealthIT` – sprawdza, że H2 działa, encja się zapisuje
- [ ] Skonfiguruj GitHub Actions workflow `.github/workflows/ci.yml` (Maven build + tests on push)
- [ ] Workflow zielony na `main` (testy używają H2, więc nie trzeba MariaDB w CI)

#### Definition of Done dla Sprintu 0
- [ ] `mvn clean verify` przechodzi lokalnie (testy na H2)
- [ ] `mvn spring-boot:run -Dspring-boot.run.profiles=dev` startuje na MariaDB bez błędów
- [ ] Swagger UI dostępny pod `/swagger-ui.html`
- [ ] README pozwala wystartować projekt komuś z zewnątrz
- [ ] Tag git `sprint-0-done`

---

### Sprint 1 – Auth + użytkownicy + role (1 tydzień)

**Cel:** Pełen flow: rejestracja → logowanie → JWT → ograniczanie endpointów po roli. Trzy role: `ADMIN`, `BUILDING_MANAGER`, `RESIDENT`. Pierwszy seed (admin).

#### Model
- [ ] Enum `Role { ADMIN, BUILDING_MANAGER, RESIDENT }`
- [ ] Klasa `AuditingEntity` (`@MappedSuperclass`) z `createdAt`, `updatedAt` + `@EnableJpaAuditing` w configu
- [ ] Encja `User extends AuditingEntity` z `@Entity`, `@Table(name="users")`, polami: `id`, `username` (`@Column(unique=true)`), `email` (`@Column(unique=true)`), `passwordHash`, `firstName`, `lastName`, `role` (`@Enumerated(EnumType.STRING)`), `enabled`
- [ ] `UserRepository extends JpaRepository<User, Long>` z metodami `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`
- [ ] Uruchom dev – Hibernate utworzy tabelę `users` automatycznie

#### DataSeeder (pierwsza wersja)
- [ ] Klasa `DataSeeder implements CommandLineRunner`, `@Component`, `@Profile("dev")`
- [ ] W `run(...)`:
  - [ ] Sprawdź `if (userRepository.count() > 0) return;`
  - [ ] Stwórz admina: `username="admin"`, `password="admin123"` (zhashowane BCryptem), rola `ADMIN`
  - [ ] Loguj `INFO: DataSeeder created admin user`
- [ ] Uruchom aplikację → admin trafia do bazy
- [ ] Restart → nic się nie dzieje (bo userów już > 0)

#### Spring Security + JWT
- [ ] Klasa `JwtService` – generowanie i walidacja tokenu (HS256, secret z `application.yml`, TTL 1h)
- [ ] Klasa `JwtAuthFilter extends OncePerRequestFilter` – wyciąga token z headera, ładuje usera, ustawia `SecurityContext`
- [ ] `SecurityConfig` z `SecurityFilterChain`:
  - [ ] CSRF off (stateless)
  - [ ] Sesja `STATELESS`
  - [ ] CORS skonfigurowany
  - [ ] `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` → `permitAll`
  - [ ] reszta → `authenticated`
  - [ ] `JwtAuthFilter` przed `UsernamePasswordAuthenticationFilter`
- [ ] `PasswordEncoder` jako `BCryptPasswordEncoder` w bean-ie
- [ ] `UserDetailsService` impl ładujący po `username`

#### Endpointy auth
- [ ] DTO: `RegisterRequest`, `LoginRequest`, `JwtResponse`, `UserResponse`
- [ ] `AuthController`:
  - [ ] `POST /api/auth/register` – walidacja, hashowanie hasła, domyślna rola RESIDENT, zwraca 201
  - [ ] `POST /api/auth/login` – walidacja, zwraca JWT + dane usera
  - [ ] `GET /api/auth/me` – zwraca aktualnego użytkownika z kontekstu
- [ ] `AuthService` – logika rejestracji/logowania, obsługa duplikatów
- [ ] Walidacja `@Valid` + `@NotBlank`, `@Email`, `@Size(min=8)` na hasło
- [ ] `GlobalExceptionHandler` z `@RestControllerAdvice` – formatuje błędy do `ApiError`

#### Endpointy zarządzania użytkownikami (admin)
- [ ] `UserController`:
  - [ ] `GET /api/users` – `@PreAuthorize("hasRole('ADMIN')")`, paginacja
  - [ ] `GET /api/users/{id}` – ADMIN lub własny profil
  - [ ] `PATCH /api/users/{id}` – update danych (bez hasła)
  - [ ] `PATCH /api/users/{id}/role` – tylko ADMIN
  - [ ] `DELETE /api/users/{id}` – tylko ADMIN, soft delete (`enabled=false`)
- [ ] `UserMapper` (MapStruct) `User → UserResponse` (bez `passwordHash`)

#### Testy
- [ ] Unit: `JwtServiceTest` – generuje, parsuje, wykrywa wygasły, wykrywa niepoprawny podpis
- [ ] Unit: `AuthServiceTest` – mockowane repo + encoder, scenariusze: sukces, duplikat, złe hasło
- [ ] Unit: `UserMapperTest`
- [ ] Integration: `AuthControllerIT` – pełen flow rejestracja → login → /me z tokenem (na H2)
- [ ] Integration: `UserControllerIT` – ADMIN widzi wszystkich, RESIDENT dostaje 403
- [ ] Pokrycie modułu auth+user > 80%

#### Definition of Done dla Sprintu 1
- [ ] Można się zarejestrować, zalogować, dostać JWT, użyć go do wywołania `/me`
- [ ] ADMIN ma dostęp do `/api/users`, RESIDENT dostaje 403
- [ ] Seed tworzy admina przy pierwszym starcie
- [ ] Wszystkie testy zielone
- [ ] Tag `sprint-1-done`

---

### Sprint 2 – Pomieszczenia + urządzenia (1 tydzień)

**Cel:** Pełne CRUD na pomieszczeniach i urządzeniach + zmiana stanu urządzenia. Rozbudowany seed.

#### Pomieszczenia
- [ ] Encja `Room extends AuditingEntity`: `id`, `name` (unique), `description`, `floor`, `area`
- [ ] DTO: `RoomRequest`, `RoomResponse`
- [ ] `RoomMapper` (MapStruct)
- [ ] `RoomRepository extends JpaRepository<Room, Long>` + `existsByName`
- [ ] `RoomService` – CRUD + walidacja unikalnej nazwy
- [ ] `RoomController`:
  - [ ] `GET /api/rooms` – wszystkie role
  - [ ] `GET /api/rooms/{id}` – wszystkie role
  - [ ] `POST /api/rooms` – ADMIN, BUILDING_MANAGER
  - [ ] `PUT /api/rooms/{id}` – ADMIN, BUILDING_MANAGER
  - [ ] `DELETE /api/rooms/{id}` – ADMIN
- [ ] Testy unit + integration

#### Urządzenia
- [ ] Enum `DeviceType { LIGHT, HEATER, AIR_CONDITIONER, FAN, BLINDS, LOCK, OUTLET, SPEAKER }`
- [ ] Enum `DeviceStatus { ON, OFF, OFFLINE, ERROR }`
- [ ] Encja `Device extends AuditingEntity`: `id`, `name`, `type` (`@Enumerated(STRING)`), `room` (`@ManyToOne(fetch=LAZY)`), `status`, `properties` (`@Column(columnDefinition="TEXT")`)
- [ ] DTO: `DeviceRequest`, `DeviceResponse`, `ChangeStateRequest`
- [ ] `DeviceMapper`
- [ ] `DeviceRepository` z metodami `findByRoom_Id`, `findByType`, `findByStatus`
- [ ] `DeviceService` – CRUD
- [ ] `DeviceStateService` – `changeState(id, newStatus, actor)`:
  - [ ] Waliduje przejście (np. nie można ustawić ON na OFFLINE)
  - [ ] Aktualizuje encję
  - [ ] Tworzy wpis `EventLog` (po Sprincie 5; na razie TODO komentarz)
  - [ ] Publikuje zdarzenie domenowe `DeviceStateChangedEvent` (Spring `ApplicationEventPublisher`)
- [ ] `DeviceController`:
  - [ ] `GET /api/devices` – paginacja, filtry: `roomId`, `type`, `status`
  - [ ] `GET /api/rooms/{roomId}/devices`
  - [ ] `GET /api/devices/{id}`
  - [ ] `POST /api/rooms/{roomId}/devices` – ADMIN, BUILDING_MANAGER
  - [ ] `PUT /api/devices/{id}` – ADMIN, BUILDING_MANAGER
  - [ ] `DELETE /api/devices/{id}` – ADMIN
  - [ ] `POST /api/devices/{id}/state` – wszystkie role (RESIDENT może sterować)

#### Rozbudowa DataSeedera
- [ ] Po userze admin – jeśli `roomRepository.count() == 0`:
  - [ ] Stwórz 4 przykładowe pokoje: "Salon", "Kuchnia", "Sypialnia", "Biuro"
  - [ ] Dla każdego pokoju 2-3 urządzenia różnych typów (LIGHT + HEATER + opcjonalnie BLINDS/AIR_CONDITIONER)
  - [ ] Dodatkowy user `manager` (rola `BUILDING_MANAGER`) i `resident` (rola `RESIDENT`) z hasłem `password123`
- [ ] Sprawdź: po świeżym uruchomieniu są 3 userzy + 4 pokoje + ~10 urządzeń

#### Testy
- [ ] Unit: `RoomServiceTest`, `DeviceServiceTest`, `DeviceStateServiceTest`
- [ ] Integration: `RoomControllerIT`, `DeviceControllerIT` – pełen flow z H2
- [ ] Test sprawdzający, że RESIDENT nie może utworzyć pokoju (403)
- [ ] Test sprawdzający, że RESIDENT może zmienić stan urządzenia
- [ ] Pokrycie modułów > 80%

#### Definition of Done dla Sprintu 2
- [ ] CRUD na rooms i devices działa z poziomu Swagger UI
- [ ] Zmiana stanu urządzenia publikuje event aplikacyjny (logowany)
- [ ] Seed tworzy realistyczny zestaw danych do testów ręcznych
- [ ] Tag `sprint-2-done`

---

### Sprint 3 – Czujniki + symulator + WebSocket (1–1,5 tygodnia)

**Cel:** Najgrubszy sprint. Czujniki, generowanie odczytów co X sekund, WebSocket broadcasty, historia odczytów z paginacją.

#### Model czujników
- [ ] Enum `SensorType { TEMPERATURE, HUMIDITY, MOTION, DOOR_WINDOW }`
- [ ] Encja `Sensor extends AuditingEntity`: `id`, `name`, `type`, `room` (`@ManyToOne`), `device` (`@ManyToOne`, nullable), `unit`, `enabled`
- [ ] Encja `SensorReading`: `id` (BIGINT), `sensor` (`@ManyToOne`), `value` (`@Column(precision=10, scale=3)`), `valueText`, `recordedAt`
- [ ] Adnotacja `@Table(name="sensor_readings", indexes = @Index(name="idx_readings_sensor_time", columnList="sensor_id, recordedAt DESC"))` – kluczowy indeks
- [ ] DTO: `SensorRequest`, `SensorResponse`, `ReadingResponse`, `SensorStatsResponse`
- [ ] Mapper, repozytoria

#### Symulator
- [ ] Klasa `ReadingGenerator`:
  - [ ] `generateFor(sensor)` – zwraca losową wartość zależnie od typu:
    - TEMPERATURE: 18.0–26.0 °C, plynne fluktuacje (poprzednia ± 0,5)
    - HUMIDITY: 30–70%, plynne fluktuacje
    - MOTION: bool z prawdopodobieństwem 15%
    - DOOR_WINDOW: bool z prawdopodobieństwem 5% zmiany stanu
  - [ ] Trzymaj poprzednią wartość per sensor w pamięci (`Map<Long, Double>`)
- [ ] Klasa `SensorSimulator`:
  - [ ] `@ConditionalOnProperty(prefix="app.simulator", name="enabled", havingValue="true")`
  - [ ] `@Scheduled(fixedDelayString = "${app.simulator.interval-ms:5000}")`
  - [ ] Pobiera wszystkie `enabled=true` czujniki
  - [ ] Generuje odczyt, zapisuje (`saveAll` w batchach)
  - [ ] Publikuje event `SensorReadingCreatedEvent`
- [ ] Konfiguracja: `app.simulator.enabled` (dev=true, test=false), `app.simulator.interval-ms: 5000`

#### WebSocket
- [ ] Klasa `WebSocketConfig implements WebSocketMessageBrokerConfigurer`:
  - [ ] `registerStompEndpoints` – `/ws` z SockJS fallback
  - [ ] `configureMessageBroker` – `enableSimpleBroker("/topic", "/queue")`, prefix `/app`
  - [ ] Interceptor walidujący JWT z handshake'a (header `Authorization` lub query param)
- [ ] Klasa `WebSocketBroadcaster` (wstrzyknięty `SimpMessagingTemplate`):
  - [ ] `broadcastSensorReading(reading)` → `/topic/sensors/{id}/readings`
  - [ ] `broadcastDeviceStateChange(device)` → `/topic/devices/{id}/state`
  - [ ] `broadcastAlert(alert)` → `/topic/alerts`
- [ ] Listener na `SensorReadingCreatedEvent` → wywołuje broadcastera
- [ ] Listener na `DeviceStateChangedEvent` → wywołuje broadcastera
- [ ] Stała `Topics` z nazwami topiców (zero magic stringów)

#### Endpointy czujników
- [ ] `SensorController`:
  - [ ] `GET /api/sensors` – paginacja, filtry
  - [ ] `GET /api/rooms/{roomId}/sensors`
  - [ ] `GET /api/sensors/{id}`
  - [ ] `POST /api/sensors` – ADMIN, BUILDING_MANAGER
  - [ ] `DELETE /api/sensors/{id}` – ADMIN
  - [ ] `GET /api/sensors/{id}/readings?from=...&to=...&page=...&size=...` – historia, max 1000 rekordów
  - [ ] `GET /api/sensors/{id}/readings/latest` – ostatni odczyt
  - [ ] `GET /api/sensors/{id}/stats?from=...&to=...` – min/max/avg

#### Rozbudowa seedera
- [ ] Dla każdego pokoju dodaj 2 czujniki: TEMPERATURE + HUMIDITY (a w sypialni dodatkowo MOTION, w drzwiach DOOR_WINDOW)
- [ ] Po starcie: ~8-10 czujników, symulator zaczyna od razu generować odczyty

#### Testy
- [ ] Unit: `ReadingGeneratorTest` – sprawdza zakresy wartości per typ
- [ ] Unit: `SensorSimulatorTest` – mockowane repo + broadcaster, sprawdza że odczyt został zapisany i wyemitowany
- [ ] Unit: `SensorReadingServiceTest` – zapytania o historię z filtrami czasowymi
- [ ] Integration: `SensorControllerIT` – CRUD + historia (na H2)
- [ ] Integration: `WebSocketIT` – klient STOMP łączy się, subskrybuje topic, dostaje wiadomość po wstrzyknięciu odczytu
- [ ] Integration: full flow – wstrzyknij odczyt → sprawdź że wisi na `/topic/sensors/{id}/readings`
- [ ] Pokrycie > 75% (WebSocket trochę zaniża)

#### Definition of Done dla Sprintu 3
- [ ] Symulator generuje dane, widać je w bazie MariaDB
- [ ] Klient WebSocket (np. wtyczka VS Code lub testowy script Node) odbiera odczyty na żywo
- [ ] Historia odczytów paginuje się
- [ ] Tag `sprint-3-done`

---

### Sprint 4 – Reguły automatyzacji + harmonogramy (1 tydzień)

**Cel:** Reguły typu "jeśli X, zrób Y" + harmonogramy CRON-owe per urządzenie.

#### Reguły automatyzacji
- [ ] Encja `AutomationRule extends AuditingEntity`: `id`, `name`, `enabled`, `sensor` (FK), `operator` (enum `LT, LTE, GT, GTE, EQ, NEQ`), `threshold` (`@Column(precision=10, scale=3)`), `targetDevice` (FK), `actionType` (enum `TURN_ON, TURN_OFF, SET_PROPERTY`), `actionPayload` (`@Column(columnDefinition="TEXT")`), `createdBy` (FK)
- [ ] DTO: `RuleRequest`, `RuleResponse`
- [ ] `AutomationRuleRepository`
- [ ] `AutomationService` – CRUD + włącz/wyłącz
- [ ] `RuleEvaluator`:
  - [ ] `evaluate(rule, latestReading)` – zwraca `true/false`
  - [ ] Obsługuje wszystkie operatory
  - [ ] Edge cases: brak odczytu, MOTION z thresholdem nie ma sensu → walidacja przy zapisie
- [ ] `RuleEngine` – listener na `SensorReadingCreatedEvent`:
  - [ ] Pobiera reguły dla danego sensora (cache w pamięci, refresh co 30s)
  - [ ] Dla każdej spełnionej: wywołuje `DeviceStateService.changeState(...)`
  - [ ] Zapobiega flapping (debounce 30 s per reguła)
- [ ] `AutomationController`:
  - [ ] `GET /api/automation/rules` – ADMIN, BUILDING_MANAGER
  - [ ] `POST /api/automation/rules` – ADMIN, BUILDING_MANAGER
  - [ ] `GET /api/automation/rules/{id}`
  - [ ] `PUT /api/automation/rules/{id}`
  - [ ] `DELETE /api/automation/rules/{id}`
  - [ ] `POST /api/automation/rules/{id}/toggle`

#### Harmonogramy
- [ ] Encja `Schedule extends AuditingEntity`: `id`, `name`, `device` (FK), `cronExpression`, `actionType`, `actionPayload`, `enabled`
- [ ] Walidacja CRON-a (`org.springframework.scheduling.support.CronExpression.parse(...)`)
- [ ] `ScheduleService` – CRUD
- [ ] `ScheduleExecutor`:
  - [ ] Wariant A (prostszy): `@Scheduled(fixedDelay = 60_000)` co minutę przegląda harmonogramy i odpala te, których `next fire time` mieści się w ostatniej minucie
  - [ ] Wariant B (lepszy): integracja z Quartz – każdy harmonogram = `JobDetail + Trigger`, on/off przez `Scheduler.pauseJob/resumeJob`
  - [ ] Decyzja: zacznij od A, jeśli starczy czasu – B
- [ ] `ScheduleController`:
  - [ ] `GET /api/schedules`
  - [ ] `POST /api/schedules`
  - [ ] `PUT /api/schedules/{id}`
  - [ ] `DELETE /api/schedules/{id}`
  - [ ] `POST /api/schedules/{id}/toggle`

#### Rozbudowa seedera
- [ ] Dodaj 2 przykładowe reguły: "Włącz grzejnik gdy temp < 18°C", "Włącz światło przy ruchu"
- [ ] Dodaj 1 przykładowy harmonogram: "Zgaś światła w salonie o 23:00"

#### Testy
- [ ] Unit: `RuleEvaluatorTest` – tabela testowa: każdy operator × każdy typ czujnika × edge cases
- [ ] Unit: `RuleEngineTest` – mockowane DeviceStateService, sprawdza że odpala się tylko gdy warunek spełniony, debounce działa
- [ ] Unit: `ScheduleExecutorTest` – mockowany czas (Clock), sprawdza wybór harmonogramów
- [ ] Integration: pełen flow – reguła "jeśli temp < 18 → włącz HEATER" → wstrzyknij odczyt 17 → device ma status ON
- [ ] Integration: harmonogram wyłącza światło o 23:00 (test z mockowanym czasem)

#### Definition of Done dla Sprintu 4
- [ ] Można utworzyć regułę z UI / Swaggera, symulator daje odczyt, urządzenie samo zmienia stan
- [ ] Harmonogram odpala akcję o zadanej godzinie
- [ ] Tag `sprint-4-done`

---

### Sprint 5 – Alerty + historia zdarzeń + raporty (1 tydzień)

**Cel:** Alerty (nietypowe zdarzenia), pełna historia działań, raporty z agregatami.

#### Alerty
- [ ] Enum `AlertType { THRESHOLD_EXCEEDED, DEVICE_OFFLINE, MOTION_DETECTED_NIGHT, DOOR_OPEN_TOO_LONG, SYSTEM_ERROR }`
- [ ] Enum `AlertSeverity { INFO, WARNING, CRITICAL }`
- [ ] Encja `Alert`: `id`, `type`, `severity`, `message`, `sourceType` (`SENSOR`/`DEVICE`/`SYSTEM`), `sourceId`, `acknowledged`, `acknowledgedBy` (FK nullable), `acknowledgedAt`, `createdAt`
- [ ] `AlertRepository` z `findByAcknowledgedFalseOrderByCreatedAtDesc`, paginacja
- [ ] `AlertService` – CRUD + acknowledge
- [ ] `AlertDispatcher`:
  - [ ] Listener na `SensorReadingCreatedEvent` – wykrywa anomalia (np. temp > 35 = krytyczne)
  - [ ] Listener na `DeviceStateChangedEvent` – status `ERROR` lub `OFFLINE` → alert
  - [ ] Tworzy `Alert`, broadcastuje na `/topic/alerts`
  - [ ] Deduplikacja: nie generuj duplikatu jeśli ten sam typ+source w ostatnich 5 min
- [ ] `AlertController`:
  - [ ] `GET /api/alerts` – paginacja, filtr `acknowledged`
  - [ ] `GET /api/alerts/{id}`
  - [ ] `POST /api/alerts/{id}/acknowledge`
  - [ ] `DELETE /api/alerts/{id}` – ADMIN

#### Historia zdarzeń (event log)
- [ ] Enum `EventType { USER_LOGIN, USER_LOGOUT, USER_REGISTERED, ROOM_CREATED, ROOM_UPDATED, ROOM_DELETED, DEVICE_CREATED, DEVICE_STATE_CHANGED, DEVICE_DELETED, RULE_CREATED, RULE_TRIGGERED, SCHEDULE_EXECUTED, ALERT_RAISED, ALERT_ACKNOWLEDGED }`
- [ ] Encja `EventLog`: `id` (BIGINT), `eventType`, `user` (FK nullable), `entityType`, `entityId`, `payload` (TEXT), `occurredAt`
- [ ] Adnotacja `@Table(name="event_log", indexes = {@Index(columnList="eventType, occurredAt DESC"), @Index(columnList="user_id, occurredAt DESC")})`
- [ ] `EventLogRepository`
- [ ] `EventLogService.log(eventType, user, entityType, entityId, payload)`
- [ ] **Listenery aplikacyjne** rozsiane po module – każdy znaczący event woła `EventLogService.log(...)`:
  - [ ] login/logout w `AuthService`
  - [ ] CRUD w `RoomService`, `DeviceService`, `AutomationService`, `ScheduleService`
  - [ ] `DeviceStateChangedEvent` listener
  - [ ] `RuleTriggeredEvent` (nowy event!)
  - [ ] `AlertRaisedEvent`
- [ ] `EventLogController`:
  - [ ] `GET /api/events?type=...&userId=...&from=...&to=...&page=...&size=...` – ADMIN, BUILDING_MANAGER

#### Raporty
- [ ] DTO: `SensorSummary` (sensorId, name, min, max, avg, count, period), `DeviceActivity` (deviceId, totalSwitches, timeOn, timeOff), `SystemHealth` (totalDevices, onlineDevices, activeAlerts, lastReadings)
- [ ] `ReportService`:
  - [ ] `getSensorSummary(sensorId, from, to)` – agregat (`SELECT MIN/MAX/AVG/COUNT FROM readings`)
  - [ ] `getDeviceActivity(deviceId, from, to)` – z event_log policz przejścia stanów
  - [ ] `getSystemHealth()` – aktualne statystyki
- [ ] `ReportController`:
  - [ ] `GET /api/reports/sensor-summary?sensorId=...&from=...&to=...`
  - [ ] `GET /api/reports/device-activity?deviceId=...&from=...&to=...`
  - [ ] `GET /api/reports/system-health`

#### Testy
- [ ] Unit: `AlertDispatcherTest` – wszystkie scenariusze, deduplikacja
- [ ] Unit: `EventLogServiceTest`
- [ ] Unit: `ReportServiceTest` – agregaty na danych testowych
- [ ] Integration: pełen flow alertu (czujnik temp > 35 → alert + wiadomość WS)
- [ ] Integration: event_log zapisuje wszystkie istotne akcje
- [ ] **Uwaga:** część zapytań agregujących może działać inaczej na H2 vs MariaDB – jeśli używasz `extract(...)` lub funkcji datetime, sprawdź obie

#### Definition of Done dla Sprintu 5
- [ ] Alerty pojawiają się w dashboardzie (kanał WS) i w historii
- [ ] Każda akcja użytkownika i systemu ląduje w `event_log`
- [ ] Raporty zwracają sensowne agregaty
- [ ] Tag `sprint-5-done`

---

### Sprint 6 – Testy E2E + dokumentacja + finalne porządki (1 tydzień)

**Cel:** Domknąć projekt: testy end-to-end pokrywające najważniejsze user journeys, kompletna dokumentacja, gotowy do uruchomienia JAR.

#### Testy E2E (RestAssured)
- [ ] Klasa bazowa `E2ETestBase` z `@SpringBootTest(webEnvironment=RANDOM_PORT)`, profil `test`, H2
- [ ] Scenariusz 1: `AdminFullJourneyE2E` – admin loguje się → tworzy pokój → dodaje urządzenie → dodaje czujnik → tworzy regułę → reguła się odpala → alert wygenerowany → admin kwituje alert
- [ ] Scenariusz 2: `ResidentJourneyE2E` – mieszkaniec loguje się → widzi pokoje → próbuje utworzyć pokój (403) → zmienia stan urządzenia (200)
- [ ] Scenariusz 3: `BuildingManagerJourneyE2E` – manager tworzy harmonogram → harmonogram odpala się → device zmienia stan → event_log zarejestrował
- [ ] Scenariusz 4: `WebSocketE2E` – pełen flow: klient łączy się WS → subskrybuje → dostaje odczyty → reguła odpala → klient dostaje update stanu urządzenia
- [ ] Scenariusz 5: `SecurityE2E` – brak tokenu = 401, zły token = 401, token ADMINa = ok, token RESIDENTa na `/api/users` = 403

#### Dokumentacja
- [ ] Pełen Swagger – każdy endpoint z `@Operation`, `@ApiResponse`, przykładami
- [ ] `README.md`:
  - [ ] Opis projektu
  - [ ] Wymagania (JDK 21, Maven, MariaDB 11)
  - [ ] Jak zainstalować i skonfigurować MariaDB (komendy SQL z sekcji 5)
  - [ ] Quick start: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` + otwórz Swagger
  - [ ] Struktura katalogów (link do `plan-pracy-backend.md`)
  - [ ] Domyślne konta z seedera (admin/admin123, manager/password123, resident/password123)
  - [ ] Jak uruchomić testy (`mvn test`)
  - [ ] Jak włączyć/wyłączyć symulator (`app.simulator.enabled` w `application-dev.yml`)
  - [ ] Jak zbudować JAR-a do uruchomienia na innej maszynie (`mvn clean package` → `target/*.jar`)
- [ ] Diagram architektury (ASCII lub mermaid w README)
- [ ] Diagram ERD z bazy (wygeneruj z DBeaver lub IntelliJ po świeżym uruchomieniu)
- [ ] Plik `CHANGELOG.md` – per sprint
- [ ] Plik `TROUBLESHOOTING.md` – częste problemy (np. "MariaDB nie wstaje", "JWT secret za krótki", "ddl-auto nie usuwa kolumn")

#### Build i dystrybucja
- [ ] `mvn clean package` produkuje uruchamialny JAR (`spring-boot-maven-plugin` repackage)
- [ ] Test: `java -jar target/smart-building-backend-*.jar --spring.profiles.active=dev` startuje aplikację
- [ ] Instrukcja w README jak uruchomić JAR na innej maszynie (wymaga JDK 21 + lokalnej MariaDB)

#### Hardening
- [ ] Globalny rate limiting na `/api/auth/login` (Bucket4j) – zabezpieczenie przed brute force
- [ ] Logi audytowe na poziomie INFO (kto, co, kiedy)
- [ ] Sprawdź wszystkie TODO w kodzie – posprzątaj
- [ ] Sprawdź pokrycie testami – globalnie > 75%, krytyczne moduły > 85%
- [ ] Spotless apply – formatowanie spójne
- [ ] `mvn dependency-check:check` (OWASP) – zero CRITICAL

#### Definition of Done dla Sprintu 6
- [ ] Wszystkie scenariusze E2E zielone
- [ ] Swagger UI kompletny i czytelny
- [ ] Świeży klon repo + `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (po setupie MariaDB) = działa
- [ ] README pozwala komuś z zewnątrz uruchomić projekt w 10-15 minut
- [ ] Tag `sprint-6-done` + `v1.0.0`

---

## 8. Definition of Done

Każdy task uznaje się za zakończony, gdy:

- [ ] Kod kompiluje się bez warningów
- [ ] Wszystkie testy (unit + integration) zielone na H2
- [ ] Pokrycie kodu nie spada poniżej progu sprintu
- [ ] Endpointy mają walidację wejścia (`@Valid`)
- [ ] Endpointy mają autoryzację (`@PreAuthorize` lub konfiguracja w `SecurityConfig`)
- [ ] Każdy endpoint udokumentowany w Swaggerze
- [ ] Każdy nowy endpoint ma test integracyjny
- [ ] Każda nowa logika biznesowa ma test jednostkowy
- [ ] Code style spójny (`mvn spotless:check`)
- [ ] Brak `TODO` bez issue na GitHubie
- [ ] Commit zgodny z Conventional Commits
- [ ] PR opisany (co, dlaczego, jak testować)
- [ ] Nowe encje sprawdzone na MariaDB lokalnie (czasem H2 i MariaDB różnie traktują typy/zapytania)

---

## 9. Strategia testowania

### Piramida testów

```
       /\
      /E2E\         ~5–10 scenariuszy (RestAssured + H2)
     /------\
    / Integr.\      ~30–50 testów (@SpringBootTest + H2)
   /----------\
  / Unit tests \    100+ testów (JUnit 5 + Mockito)
 /--------------\
```

### Testy jednostkowe
- Cel: izolowane jednostki logiki (serwisy, ewaluatory, mappery)
- Narzędzia: JUnit 5, Mockito, AssertJ
- Konwencja: `*Test.java` w `src/test/java/.../unit/`
- Cechy: szybkie (cały zestaw < 30s), bez Springa, bez bazy

### Testy integracyjne
- Cel: zachowanie modułu w kontekście Springa, z prawdziwą bazą (H2 w trybie MariaDB)
- Narzędzia: `@SpringBootTest`, H2 (mode=MariaDB), MockMvc
- Konwencja: `*IT.java` w `src/test/java/.../integration/`
- Każdy controller ma swój `*ControllerIT`
- **Pułapka:** H2 w trybie MariaDB obsługuje większość, ale nie wszystko. Jeśli używasz funkcji specyficznych dla MariaDB (np. `JSON_EXTRACT`, niektóre funkcje datetime), test może przejść na H2 i paść na produkcyjnej MariaDB. W razie wątpliwości – uruchom kluczowe testy ręcznie też na MariaDB.

### Testy E2E
- Cel: pełen user journey przez prawdziwy HTTP
- Narzędzia: RestAssured, H2
- Konwencja: `*E2E.java` w `src/test/java/.../e2e/`
- Uruchamiane osobnym profilem Maven (`mvn verify -Pe2e`)

### Pokrycie
- Globalnie: ≥ 75%
- Logika biznesowa (services, evaluators): ≥ 85%
- Controllery: ≥ 70% (resztę pokrywają testy integracyjne)
- Konfiguracja Springa, DTO, mappery generowane: pomijalne

### CI
- Każdy push i PR uruchamia: `mvn clean verify` (unit + integration na H2 – nie wymaga zewnętrznej bazy!)
- E2E uruchamiane osobno (cron nightly lub na PR z labelem `e2e`)
- CI nie wymaga MariaDB ani Dockera – wszystko działa na H2

---

## 10. Ryzyka i punkty uwagi

| Ryzyko | Prawdopodobieństwo | Mitygacja |
|---|---|---|
| Spring Boot 4.x to świeża wersja, część bibliotek może nie być kompatybilna | średnie | Sprawdź kompatybilność na początku Sprintu 0; w razie blokerów spadnij na Spring Boot 3.4 |
| `ddl-auto=update` nie usuwa starych kolumn / nie przemianowuje – w razie poważnej zmiany schematu zostają śmieci | wysokie | W dev: `DROP DATABASE smart_building; CREATE DATABASE smart_building;` + restart aplikacji; seed odtworzy dane |
| H2 i MariaDB różnie obsługują niektóre zapytania (test zielony, prod czerwony) | średnie | Tryb `MODE=MariaDB` w H2 pomaga; raz w sprincie odpal kluczowe testy też na MariaDB lokalnie |
| Symulator zalewa bazę odczytami | wysokie | Konfigurowalny interwał, batch insert; opcjonalnie cron czyszczący starsze niż 30 dni |
| WebSocket + JWT – walidacja na handshake'u jest nietrywialna | średnie | Zarezerwuj dzień na samego WebSocketa, zacznij od działającego flow bez bezpieczeństwa, potem dołóż auth |
| Reguły automatyzacji odpalają się w pętli (flapping) | wysokie | Debounce 30s per reguła, idempotentność akcji |
| Quartz jako "nice to have" wycieka time'u | średnie | Zacznij od `@Scheduled`, Quartz tylko jeśli starczy czasu |
| Synchronizacja zegarów w testach z mockowanym czasem | niskie | `Clock` jako bean (nie statyczny `LocalDateTime.now()`) |
| Wysokie pokrycie testami WS jest trudne | średnie | Jeden solidny test integracyjny + manualne smoke testy |
| Frontend pyta o coś, czego nie ma w API | wysokie | Aktualizuj `model-danych-i-api.md` na bieżąco; tygodniowy sync z Danielem |
| Kolega ma inny system / inną wersję MariaDB | średnie | Wspólnie ustalony numer wersji w README; wskazówki dla Win/Mac/Linux |

### Punkty kontrolne (raz w tygodniu)
- Czy projekt nadal się buduje na czystym kloningu?
- Czy testy nie są wyłączone?
- Czy pokrycie nie spada?
- Czy frontend ma wszystko czego potrzebuje?
- Czy Swagger jest aktualny?
- Czy schemat bazy w MariaDB nie zawiera "śmieci" po starych encjach?

---

**Powiązane dokumenty:**
- [`model-danych-i-api.md`](./model-danych-i-api.md) – pełen model danych + lista endpointów REST + topiki WebSocket
- Dokument projektowy grupowy (PDF) – wymagania funkcjonalne i niefunkcjonalne
