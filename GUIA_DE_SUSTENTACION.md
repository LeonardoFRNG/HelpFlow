# 🎓 GUÍA MAESTRA DE ESTUDIO Y DEFENSA TÉCNICA — HELPFLOW
## Preparación Integral para la Sustentación de la Prueba de Desempeño M6.1 Java + Spring Boot

Esta guía ha sido diseñada para darte **absoluta seguridad técnica** durante tu sustentación. Contiene la explicación detallada de cada capa arquitectónica, decisiones de diseño, algoritmos, comandos clave y un banco exhaustivo de preguntas y respuestas difíciles que los evaluadores suelen formular.

---

## TABLA DE CONTENIDOS

1. [Elevator Pitch (Cómo presentar el proyecto en 2 minutos)](#1-elevator-pitch)
2. [Arquitectura Hexagonal Explicada con Manzanas (Paso a Paso)](#2-arquitectura-hexagonal-a-fondo)
3. [El Flujo de una Petición en HelpFlow (End-to-End Trace)](#3-el-flujo-de-una-petición-en-helpflow)
4. [Microservicios y el Algoritmo Determinista de Despacho](#4-microservicios-y-algoritmo-de-despacho)
5. [Máquina de Estados e Invariantes del Dominio](#5-máquina-de-estados-e-invariantes)
6. [Gestión de SLA y Motor de Escalamiento Programado](#6-gestión-de-sla-y-escalamiento)
7. [Base de Datos, Estrategia Flyway y Optimización JPA](#7-base-de-datos-flyway-y-jpa)
8. [Seguridad Stateless JWT y Control de Acceso (RBAC)](#8-seguridad-jwt-y-rbac)
9. [Observabilidad, MDC (Trace ID) y Métricas Micrometer](#9-observabilidad-y-métricas)
10. [Estrategia de Pruebas: Testcontainers vs Mocks](#10-estrategia-de-pruebas)
11. [Banco de Preguntas y Respuestas Difíciles (Preguntas de Sustentación)](#11-banco-de-preguntas-y-respuestas)
12. [Chuleta de Comandos Rápidos para la Demo en Vivo](#12-chuleta-de-comandos-para-la-demo)

---

## 1. ELEVATOR PITCH
*(Apréndete esto para abrir tu exposición)*

> "Buenas tardes. **HelpFlow** es una plataforma distribuida para la gestión integral de tickets e incidencias de soporte de TI, diseñada bajo los principios de **Arquitectura Hexagonal (Ports and Adapters)** y **Domain-Driven Design (DDD)**.
> 
> El sistema está compuesto por dos microservicios desacoplados:
> 1. **`support-ticket-service` (Puerto 8080):** El núcleo del sistema que orquesta el ciclo de vida del ticket, calcula y escala SLAs automáticamente, gestiona usuarios y técnicos con seguridad JWT sin estado y persiste auditorías completas en PostgreSQL con Flyway.
> 2. **`technician-dispatch-service` (Puerto 8081):** Un microservicio autónomo y determinista que simula la asignación y disponibilidad técnica sin sobrecarga de bases de datos.
> 
> El proyecto cuenta con **85 pruebas automatizadas pasando con 0 fallos**, cobertura JaCoCo superior al **82%**, pruebas de integración con PostgreSQL real mediante **Testcontainers**, y despliegue orquestado con **Docker Compose** listo para producción."

---

## 2. ARQUITECTURA HEXAGONAL A FONDO

### ¿Por qué Hexagonal y no la típica arquitectura por capas tradicional?
En la arquitectura por capas tradicional (`Controller -> Service -> Repository -> BD`), la capa de lógica de negocio (`Service`) suele estar fuertemente acoplada a las anotaciones de Hibernate (`@Entity`, `@Table`) y al framework (`@Transactional`, Spring). Si cambias de base de datos o de framework, la lógica se rompe.

En **Arquitectura Hexagonal (Puertos y Adaptadores)**:
- **El Dominio es el Rey:** El negocio (`Ticket`, `User`, `SlaPolicy`) se encuentra en el centro más íntimo y **NO IMPORTA NADA** de Spring, JPA, Hibernate o Web. Son clases Java puras (POJOs).
- **Inversión de Dependencias (DIP de SOLID):** La capa de aplicación define interfaces (**Puertos**) y la infraestructura técnica (Base de Datos, APIs externas, Controladores Web) implementa o consume esos puertos a través de **Adaptadores**.

```text
       [ MUNDO EXTERIOR ]                                    [ MUNDO EXTERIOR ]
      (Postman / Navegador)                                (PostgreSQL / Microservicio)
               |                                                         ^
               v                                                         |
    +----------------------+                                 +-----------------------+
    |  ADAPTADOR DE ENTRADA|                                 |  ADAPTADOR DE SALIDA  |
    |   (Driving Adapter)  |                                 |    (Driven Adapter)   |
    |   TicketController   |                                 |TicketPersistenceAdapt.|
    +----------+-----------+                                 +-----------^-----------+
               |                                                         |
         invoca|                                               implementa|
               v                                                         |
    +----------------------+                                 +-----------+-----------+
    |   PUERTO DE ENTRADA  |                                 |   PUERTO DE SALIDA    |
    |     (Input Port)     |                                 |     (Output Port)     |
    |  CreateTicketUseCase |                                 | TicketRepositoryPort  |
    +----------+-----------+                                 +-----------^-----------+
               |                                                         |
     ejecuta   |               CAPA DE APLICACIÓN                        | orquesta
               +---------------------> TicketService --------------------+
                                             |
                                     gobierna|
                                             v
                                  +---------------------+
                                  |   CAPA DE DOMINIO   |
                                  |     (El Núcleo)     |
                                  | Ticket, SlaPolicy   |
                                  +---------------------+
```

### Definición de Términos Clave:
1. **Dominio (`domain`):** Entidades puras y reglas que existirían incluso si la empresa operara en papel sin computadoras. Ejemplo: `Ticket.java`, `SlaPolicy.java`.
2. **Puerto de Entrada (`port.in`):** Las acciones que el sistema *permite hacer* desde afuera (Casos de Uso). Son interfaces Java. Ejemplo: `CreateTicketUseCase.java`.
3. **Puerto de Salida (`port.out`):** Lo que el sistema *necesita del exterior* para cumplir su trabajo. Son interfaces Java. Ejemplo: `TicketRepositoryPort.java`, `TechnicianDispatchPort.java`.
4. **Adaptador de Entrada (`adapter.in.web`):** Traduce HTTP/JSON a comandos que el puerto de entrada entiende. Ejemplo: `TicketController.java`.
5. **Adaptador de Salida (`adapter.out.persistence / dispatch`):** Traduce las órdenes de los puertos de salida a SQL/JPA o peticiones HTTP REST. Ejemplo: `TicketPersistenceAdapter.java`, `TechnicianDispatchRestClientAdapter.java`.

---

## 3. EL FLUJO DE UNA PETICIÓN EN HELPFLOW (END-TO-END TRACE)

Cuando un usuario envía un `POST /tickets` para crear un ticket:

1. **Recepción Web:** Llega a `TicketController` (Adaptador Entrada). Se valida el DTO (`@Valid CreateTicketRequestDto`).
2. **Extracción de Identidad:** El controlador extrae el usuario autenticado desde el `SecurityContextHolder` (evitando suplantaciones).
3. **Paso al Puerto de Entrada:** Se crea el comando `CreateTicketCommand` y se llama a `createTicketUseCase.createTicket(command)`.
4. **Servicio de Aplicación:** `TicketService` orquesta la transacción:
   - Valida con `UserRepositoryPort` que el usuario exista y esté `ACTIVE`.
   - Llama a la regla de dominio `SlaPolicy.calculateDeadline(priority, now)` para calcular la fecha límite según la prioridad.
   - Crea la entidad de dominio `Ticket` en estado inicial `OPEN`.
5. **Comunicación con Microservicio Externo:**
   - `TicketService` llama al puerto de salida `TechnicianDispatchPort`.
   - El adaptador `TechnicianDispatchRestClientAdapter` hace una petición REST HTTP a `technician-dispatch-service:8081`.
   - **Resiliencia:** Si el microservicio 8081 no responde o falla, el adaptador captura el error y aplica un respaldo local (`AssignmentEligibilityPolicy`), garantizando que la creación del ticket nunca se caiga.
6. **Transición de Dominio:** Con el técnico asignado, la entidad `Ticket` ejecuta su método interno `assignTechnician(...)`, cambia su estado a `ASSIGNED` y genera su primer registro en `TicketHistory`.
7. **Persistencia:** `TicketService` llama a `TicketRepositoryPort.save(ticket)`.
8. **Mapeo JPA:** `TicketPersistenceAdapter` recibe la entidad de dominio pura, la mapea a `TicketJpaEntity` y la guarda en PostgreSQL mediante `SpringDataTicketRepository`.
9. **Métricas:** Se incrementa el contador de Micrometer `helpflow.tickets.created`.
10. **Respuesta:** El controlador devuelve `201 Created` con el `TicketResponseDto` y la cabecera `X-Trace-Id`.

---

## 4. MICROSERVICIOS Y EL ALGORITMO DETERMINISTA DE DESPACHO

### Microservicio `technician-dispatch-service` (Puerto 8081)
- **Cumplimiento estricto del enunciado:** Es 100% independiente. **NO TIENE Spring Data JPA, NO TIENE Spring Security, NO TIENE base de datos.**
- Solo expone `POST /technician-dispatch`.

### Algoritmo Determinista (¿Por qué determinista?):
El enunciado prohíbe el uso de aleatoriedad descontrolada (`Math.random()`). Una misma entrada (`ticketId`, `category`, `priority`) debe producir **siempre el mismo resultado**.

**Implementación técnica:**
1. Se combinan los 3 campos en una cadena: `String input = ticketId + ":" + category + ":" + priority`.
2. Se genera un hash criptográfico **MD5**: `MessageDigest.getInstance("MD5").digest(input.getBytes())`.
3. Se extrae un número entero positivo del hash.
4. Se calcula el módulo respecto a los 20 técnicos del sistema: `(hashPositive % 20) + 1`.
5. Se calcula el tiempo estimado en horas y se clasifica la disponibilidad:
   - `0 - 2 horas` $\rightarrow$ `HIGH`
   - `3 - 8 horas` $\rightarrow$ `MEDIUM`
   - `> 8 horas` $\rightarrow$ `LOW`

---

## 5. MÁQUINA DE ESTADOS E INVARIANTES DEL DOMINIO

El ciclo de vida del ticket no son simples campos que se actualizan; es una **máquina de estados finita** protegida por métodos dentro de la entidad de dominio `Ticket.java`.

### Diagrama de Estados:
```text
[OPEN] ---> [ASSIGNED] ---> [IN_PROGRESS] ---> [RESOLVED] ---> [CLOSED]
  |              |                |
  +--------------+----------------+-------------> [CANCELLED]
```

### Reglas e Invariantes Intocables:
1. **Transición legal:** Un ticket no puede saltar de `OPEN` a `RESOLVED` directamente, ni pasar de `CLOSED` a `OPEN`. Cualquier intento lanza `InvalidStateTransitionException` que se traduce en un **HTTP 400 Bad Request**.
2. **Propiedad y Permisos (Ownership):**
   - Un usuario estándar (`ROLE_USUARIO`) solo puede ver o cancelar **sus propios tickets**.
   - Un técnico (`ROLE_TECNICO`) solo puede gestionar los tickets **asignados a él**.
   - Un administrador (`ROLE_ADMIN`) tiene potestad global.
3. **Observaciones Obligatorias:** No se puede mover de estado sin enviar una justificación u observación obligatoria en el cuerpo (`observations`), la cual se persiste en la tabla de auditoría `ticket_histories`.

---

## 6. GESTIÓN DE SLA Y ESCALAMIENTO PROGRAMADO

### Políticas de SLA Configuradas:
- **`CRITICAL`:** Plazo de resolución de **4 horas**.
- **`HIGH`:** Plazo de resolución de **8 horas**.
- **`MEDIUM`:** Plazo de resolución de **24 horas**.
- **`LOW`:** Plazo de resolución de **48 horas**.

### Motor de Escalamiento Automático (`SlaEscalationScheduler`):
1. Corre en segundo plano mediante un cron programado (`@Scheduled(cron = "0 * * * * *")`, cada 60 segundos por defecto).
2. Busca tickets activos (`OPEN`, `ASSIGNED`, `IN_PROGRESS`) que no hayan sido resueltos.
3. Si el tiempo actual entra en la ventana de riesgo:
   - `HIGH` a menos de 2 horas del vencimiento $\rightarrow$ Escala a `CRITICAL`.
   - `MEDIUM` a menos de 4 horas del vencimiento $\rightarrow$ Escala a `HIGH`.
   - `LOW` a menos de 8 horas del vencimiento $\rightarrow$ Escala a `MEDIUM`.
4. El escalamiento actualiza la prioridad, recalcula el SLA y registra un historial con `actor = SYSTEM`.
5. Si un ticket supera su plazo sin resolverse, incrementa la métrica `helpflow.tickets.overdue`.

---

## 7. BASE DE DATOS, FLYWAY Y OPTIMIZACIÓN JPA

### Migraciones Versionadas con Flyway:
No usamos `ddl-auto=update` porque en ambientes corporativos destruye o bloquea datos. Usamos scripts SQL idempotentes:
- `V1__schema.sql`: Estructura DDL base (`users`, `technicians`, `tickets`).
- `V2__assignments_history_sla.sql`: Tablas relacionales (`assignments`, `ticket_histories`), claves foráneas e índices de rendimiento en `status`, `sla_deadline` y `user_id`.
- `V3__initial_data.sql`: Carga inicial de 20 técnicos con sus especialidades y usuarios base (`admin@helpflow.com`, `tecnico@helpflow.com`, `usuario@helpflow.com`).

### Optimización contra el problema N+1 y Bags:
- **Evitar N+1:** En `SpringDataTicketRepository` se utiliza `@EntityGraph(attributePaths = {"user", "technician"})` para que las consultas traigan las entidades relacionadas en un solo `JOIN` en lugar de hacer 1 query por cada fila hija.
- **Evitar `MultipleBagFetchException`:** Las relaciones `@OneToMany` en `TicketJpaEntity` (`assignments` y `histories`) se modelaron como `Set<AssignmentJpaEntity>` (`LinkedHashSet`) en vez de `List`. Hibernate prohíbe hacer fetch simultáneo de múltiples listas no indexadas (bags) para evitar productos cartesianos en memoria.

---

## 8. SEGURIDAD JWT, REFRESH TOKENS Y CONTROL DE ACCESO (RBAC)

- **Filtro Stateless:** Spring Security configurado con `SessionCreationPolicy.STATELESS`. El servidor no guarda sesiones en memoria RAM.
- **Tokens de Acceso (Access Token):** Firmados con HMAC-SHA512. Tienen una vigencia corta (15 minutos por defecto, configurable mediante `jwt.access-token-expiration-ms`) e incluyen claims estándar RFC 7519 (`jti` único por token, `sub`, `role`, `name`, `userId`).
- **Mecanismo de Refresh Token y Rotación (RTR):**
  - **Generación criptográfica:** Se generan con `SecureRandom` con 32 bytes de entropía en Base64 URL-safe (longitud de 43 caracteres).
  - **Almacenamiento seguro (Hashing SHA-256):** Nunca se guarda el Refresh Token en texto plano en la base de datos. Solo se persiste su hash criptográfico SHA-256 en la tabla `refresh_tokens`.
  - **Rotación estricta de tokens (RTR):** En cada petición a `POST /auth/refresh`, el token actual se marca como revocado (`revoked = true`, `replaced_by_token_hash = <nuevo_hash>`) y se entrega un par completamente nuevo (nuevo Access Token y nuevo Refresh Token).
  - **Detección de ataques de repetición / Replay Attacks:** Si un atacante roba un Refresh Token ya consumido e intenta renovarlo, el sistema detecta que `isRevoked() == true` y, mediante una transacción independiente (`Propagation.REQUIRES_NEW`), **invalida y revoca inmediatamente todas las sesiones activas del usuario afectado** (`revokeAllByUserId`), mitigando cualquier acceso no autorizado posterior.
  - **Cierre de Sesión (`POST /auth/logout`):** Permite revocar voluntariamente el token persistente del usuario, invalidándolo de forma definitiva.
  - **Arquitectura Hexagonal Pura:**
    - Modelo puro: `com.helpflow.ticket.domain.model.RefreshToken` (POJO con lógica de invariantes `isExpired`, `isActive`, `revoke`, `replaceBy`).
    - Puertos: `RefreshTokenRepositoryPort` y `TokenProviderPort`.
    - Adaptadores: `RefreshTokenPersistenceAdapter` (con `@Transactional(propagation = Propagation.REQUIRES_NEW)`) y `JwtTokenProviderAdapter`.
- **Cifrado de Contraseñas:** Algoritmo **BCrypt** con 10 rondas de salt.
- **Validación de Dominio Corporativo:** La expresión regular `^[A-Za-z0-9._%+-]+@helpflow\.com$` asegura que solo personas con correo corporativo puedan registrarse. Correos externos (`@gmail.com`) devuelven `400 Bad Request`.
- **Formato Estándar de Errores (RFC 7807):** Toda excepción (validación, 401, 403, regla de negocio) es capturada por `@RestControllerAdvice` y devuelta como un JSON `ProblemDetail` que incluye: `type`, `title`, `status`, `detail`, `instance`, `timestamp` y `traceId`.

---

## 9. OBSERVABILIDAD, TRACE ID Y MÉTRICAS MICROMETER

1. **Trazabilidad Distribuida (MDC - Mapped Diagnostic Context):**
   - El filtro `TraceIdFilter` intercepta cada petición.
   - Si viene la cabecera `X-Trace-Id` la mantiene, si no, genera un UUID aleatorio.
   - Se inyecta en el MDC de SLF4J, por lo que **cada línea de log** imprime el `[traceId]`.
   - Se retorna en la cabecera HTTP de respuesta y en los errores RFC 7807.
2. **Métricas en `/actuator/metrics`:**
   - `helpflow.tickets.created`: Conteo total de tickets por categoría y prioridad.
   - `helpflow.tickets.overdue`: Tickets que rompieron el SLA.
   - `helpflow.tickets.first_response.time`: Tiempo entre creación y primera respuesta/asignación.
   - `helpflow.tickets.resolution.time`: Tiempo total hasta la resolución.
   - `helpflow.auth.failures`: Intentos de login fallidos.
   - `helpflow.auth.access_denied`: Intentos bloqueados con 403 Forbidden.

---

## 10. ESTRATEGIA DE PRUEBAS: TESTCONTAINERS VS MOCKS

- **Pruebas Unitarias del Dominio (`domain`):** Sin Spring, ultra rápidas, prueban invariantes puras de la máquina de estados y cálculo de horas de SLA usando un `Clock` simulado.
- **Pruebas de Controladores (`@WebMvcTest`):** Prueban serialización JSON, Bean Validation y respuestas HTTP aislando la lógica.
- **Pruebas de Integración con Testcontainers (`integration`):**
  - **Cero Mocks en Persistencia:** Levantamos un contenedor Docker real de PostgreSQL 16 durante los tests (`PostgresDatabaseTestcontainersTest`, `FullLifecycleIntegrationTest`).
  - Flyway ejecuta las migraciones reales contra ese PostgreSQL en Docker y se prueban transacciones, queries con `@EntityGraph`, triggers y restricciones de unicidad reales.
- **Resultados:** 85 tests ejecutados, 0 fallos, 0 errores, cobertura JaCoCo **>82%** (incluyendo suites completas de rotación, expiración, reutilización de tokens y logout en Testcontainers).

---

## 11. BANCO DE PREGUNTAS Y RESPUESTAS DIFÍCILES (PREGUNTAS DE SUSTENTACIÓN)

### P1: "¿Por qué elegiste Arquitectura Hexagonal en lugar de una arquitectura en capas tradicional?"
**Respuesta:**  
> "Porque la arquitectura en capas genera un acoplamiento directo entre la lógica de negocio y las tecnologías de base de datos o frameworks. En HelpFlow, implementé Arquitectura Hexagonal para que el dominio del negocio (`Ticket`, `User`, `SlaPolicy`) sea completamente agnóstico y portable. Si mañana decidimos cambiar PostgreSQL por MongoDB o migrar de Spring Boot a Quarkus, nuestro dominio y nuestras reglas de negocio permanecen 100% intactas, ya que solo interactúan con Puertos abstractos (interfaces) y los detalles técnicos quedan confinados en los Adaptadores."

### P2: "¿Dónde reside la lógica de negocio y por qué no está en los Controllers o en los Repositorios?"
**Respuesta:**  
> "La lógica de negocio reside estrictamente en dos lugares:
> 1. En las **Entidades de Dominio** (`com.helpflow.ticket.domain.model`): Ahí se validan los invariantes, las transiciones válidas de estado y los cálculos de SLA (`SlaPolicy`).
> 2. En los **Servicios de Aplicación** (`com.helpflow.ticket.application.service`): Donde se orquestan los casos de uso llamando a los puertos de salida.
> Los controladores solo actúan como adaptadores de transporte HTTP encargados de serializar/deserializar JSON y validar la estructura básica con Bean Validation. Los repositorios y adaptadores de persistencia son meros traductores entre el dominio y el SQL de PostgreSQL."

### P3: "¿Cómo garantizaste que el microservicio de despacho sea determinista?"
**Respuesta:**  
> "El enunciado exigía que la asignación no utilizara aleatoriedad arbitraria y que para los mismos parámetros siempre devolviera el mismo técnico. Para lograrlo, implementé una función hash con algoritmo MD5 sobre la combinación de `ticketId + category + priority`. Al valor entero positivo del hash le aplicamos una operación de módulo sobre el catálogo de los 20 técnicos (`hash % 20 + 1`). De esta forma, si envías el ticket 101 con categoría HARDWARE y prioridad HIGH, siempre resultará en el mismo técnico asignado y con el mismo cálculo de horas y disponibilidad."

### P4: "¿Qué sucede si el microservicio de despacho (puerto 8081) se cae o no responde?"
**Respuesta:**  
> "El sistema cuenta con un mecanismo de resiliencia y tolerancia a fallos en el adaptador `TechnicianDispatchRestClientAdapter`. Si el servicio 8081 experimenta un timeout o un error HTTP, la llamada es capturada dentro de un bloque de contingencia que recurre a la política local de elegibilidad (`AssignmentEligibilityPolicy`). Esto selecciona un técnico elegible directamente en la base de datos y marca la asignación como respaldo (`FALLBACK`), permitiendo que el ticket se cree exitosamente en lugar de interrumpir la operación del usuario."

### P5: "¿Cómo preveniste el problema de las consultas N+1 en Hibernate?"
**Respuesta:**  
> "El problema N+1 ocurre cuando Hibernate hace una consulta para obtener los tickets y luego realiza una consulta SQL adicional por cada fila para traer el usuario o el técnico asociado. Lo solucioné de dos maneras:
> 1. En `SpringDataTicketRepository` utilizamos anotaciones `@EntityGraph(attributePaths = {"user", "technician"})` y consultas `JOIN FETCH` explícitas, forzando a que la consulta SQL traiga las relaciones en un solo viaje a la base de datos.
> 2. Las colecciones `@OneToMany` dentro de `TicketJpaEntity` (`assignments` e `histories`) se modelaron utilizando `Set` (`LinkedHashSet`) en lugar de `List`, evitando el error `MultipleBagFetchException` de Hibernate y optimizando el uso de memoria."

### P6: "¿Cómo está estructurado el tiempo de vida de los tokens de autenticación?"
**Respuesta:**  
> "Seguimos las recomendaciones de seguridad de OWASP y RFC 6749:
> 1. **Access Token de corta duración (15 minutos):** Minimiza la ventana de exposición en caso de intercepción en red o robo en el cliente.
> 2. **Refresh Token de larga duración (7 días):** Permite al cliente renovar su sesión silenciosamente sin obligar al usuario a reintroducir sus credenciales constantemente.
> Además, los tiempos son parametrizables externamente mediante variables de entorno (`JWT_ACCESS_EXPIRATION_MS` y `JWT_REFRESH_EXPIRATION_MS`)."

### P7: "¿Cómo funciona la detección de reuso (Replay Attacks) en el Refresh Token y por qué es segura?"
**Respuesta:**  
> "Implementamos **Refresh Token Rotation (RTR)** junto con detección de brecha de seguridad:
> 1. Cada vez que un Refresh Token es utilizado, se marca como revocado (`revoked = true`) y se asocia el hash del token que lo reemplazó (`replaced_by_token_hash`).
> 2. Si un atacante interceptó un token previamente consumido e intenta usarlo para obtener un nuevo Access Token, el sistema detecta que el token ya estaba revocado.
> 3. En ese instante, asume un posible compromiso de las credenciales y, mediante una transacción inmediata con `Propagation.REQUIRES_NEW`, **revoca todos los Refresh Tokens activos de ese usuario** (`revokeAllByUserId`) y rechaza la petición con HTTP 400. Esto expulsa automáticamente al atacante y protege la cuenta del usuario."

### P8: "¿Cómo se maneja la seguridad para que un técnico no modifique un ticket que no le corresponde?"
**Respuesta:**  
> "Implementamos seguridad en dos niveles:
> 1. **Nivel de Endpoint:** Spring Security valida el rol (`ROLE_TECNICO`, `ROLE_ADMIN`, `ROLE_USUARIO`).
> 2. **Nivel de Recurso y Dominio:** En `TicketService.updateStatus`, el servicio extrae el nombre del usuario autenticado directamente del `SecurityContextHolder`. Si el rol es `ROLE_TECNICO`, se valida que el correo del técnico asignado al ticket coincida exactamente con el usuario autenticado. Si no coincide, se deniega la operación lanzando una `UnauthorizedTicketAccessException` que retorna un **HTTP 403 Forbidden**."

---

## 12. CHULETA DE COMANDOS RÁPIDOS PARA LA DEMO EN VIVO

Ten esta sección a la mano por si el evaluador te pide ejecutar o probar cosas en vivo:

### 1. Iniciar los contenedores con Docker Compose:
```bash
DOCKER_BUILDKIT=0 COMPOSE_PROJECT_NAME=helpflow docker compose up --build -d
```

### 2. Comprobar salud de los servicios:
```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

### 3. Ejecutar los 85 tests y validar cobertura en vivo:
```bash
mvn test
```

### 4. Probar en vivo el flujo de Refresh Token y Logout con cURL:
```bash
# A. Login obteniendo Access Token y Refresh Token:
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@helpflow.com","password":"Password123!"}'

# B. Renovar tokens (Refresh Token Rotation):
curl -s -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN_OBTENIDO>"}'

# C. Cerrar sesión de forma segura:
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN_ACTUAL>"}'
```

### 5. Abrir la documentación interactiva:
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Métricas Micrometer:** `http://localhost:8080/actuator/metrics`

### 6. Cuentas de Acceso Preconfiguradas:
| Rol | Email | Password |
| :--- | :--- | :--- |
| **Administrador** | `admin@helpflow.com` | `Password123!` |
| **Técnico** | `tecnico@helpflow.com` | `Password123!` |
| **Usuario** | `usuario@helpflow.com` | `Password123!` |
