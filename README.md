# Post-contenido — Unidad 5: Integración en Aplicaciones Web (ReservasLabs)

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Proyecto Spring Boot (`reservas-labs-api`) para la gestión y reserva de laboratorios de cómputo universitarios.

El sistema implementa una arquitectura en capas rigurosa (`model`, `repository`, `service`, `controller` REST y vista web MVC con Thymeleaf) sobre una base de datos H2 embebida, evitando el antipatrón de *Service Anémico* mediante reglas de negocio no triviales (validación de horarios, duraciones y solapamientos de reservas).

---

## Cómo ejecutar

### Prerrequisitos
- **Java 17** o superior configurado en el `PATH`.
- **Apache Maven 3.8+** instalado.

### Comandos de ejecución y pruebas
```bash
# Compilar y empaquetar el proyecto
mvn clean package

# Ejecutar la aplicación Spring Boot
mvn spring-boot:run

# Ejecutar las pruebas unitarias y de integración (JUnit 5)
mvn test
```

### URLs de acceso
- **API REST:** `http://localhost:8080/api/reservas` y `http://localhost:8080/api/laboratorios`
- **Consola H2:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:reservas_labs_db`, Usuario: `sa`, Contraseña en blanco)
- **Vista Web MVC (Thymeleaf):** `http://localhost:8080/reservas`

---

## Estructura del Proyecto

```text
gallo-post1-u5/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── application.properties
    │   └── java/com/universidad/reservaslabs/
    │       ├── ReservasLabsApplication.java
    │       ├── model/
    │       │   ├── Laboratorio.java
    │       │   ├── EstadoReserva.java
    │       │   └── Reserva.java
    │       ├── repository/
    │       │   ├── LaboratorioRepository.java
    │       │   └── ReservaRepository.java
    │       ├── exception/
    │       │   ├── ReservaConflictException.java
    │       │   ├── RecursoNoEncontradoException.java
    │       │   └── GlobalRestExceptionHandler.java
    │       ├── service/
    │       │   └── ReservaService.java
    │       └── controller/
    │           ├── LaboratorioController.java
    │           └── ReservaController.java
    └── test/java/com/universidad/reservaslabs/
        └── ReservaApiIntegrationTest.java
```

---

## Decisiones de diseño

### Punto de decisión 1 — Ubicación de la validación de solapamiento de horarios
* **Decisión arquitectónica:** La comprobación de solapamiento se realiza mediante una consulta JPQL explícita en `ReservaRepository.buscarSolapamientos()`, mientras que la decisión de negocio (evaluar si la lista retornada no está vacía y lanzar `ReservaConflictException`) reside exclusivamente en `ReservaService`.
* **Justificación técnica:**
  1. **Eficiencia y escalabilidad:** Si la validación se hiciera enteramente en memoria en el Service, habría que traer todas las reservas históricas del laboratorio a la memoria de la JVM (`reservaRepo.findAll()`) y comparar intervalos temporales con bucles en Java. Con cientos o miles de reservas, esta solución colapsaría el consumo de memoria y degradaría el rendimiento. La consulta JPQL delega el filtrado al motor de base de datos relacional (H2), retornando únicamente las reservas que efectivamente colisionan (`inicio < :fin AND fin > :inicio AND estado <> CANCELADA`).
  2. **Separación de responsabilidades:** El Repository responde a una pregunta de datos pura (*"¿qué reservas activas colisionan con este rango?"*), mientras que el Service decide qué significa ese resultado para el negocio (*"si hay reservas, se cancela la operación y se arroja un error 409 Conflict"*). Si el controlador llamara directamente al Repository, la regla de negocio quedaría secuestrada en la capa web, violando la arquitectura en capas.

---

### Punto de decisión 2 — Reglas con y sin apoyo del Repository
* **Decisión arquitectónica:** Separar las validaciones intrínsecas del objeto de aquellas que requieren estado relacional persistido:
  1. **Reglas puras de dominio (sin apoyo del Repository):** La validación de horario de atención institucional (07:00 a 21:00) y de duración permitida (mínimo 30 minutos, máximo 3 horas) depende exclusivamente de los atributos propios de la reserva que se intenta registrar (`inicio` y `fin`). Se ejecuta en `ReservaService.validarHorarioYDuracion()` con Java puro (`java.time.LocalTime` y `java.time.Duration`) antes de cualquier acceso a la base de datos.
  2. **Reglas relacionales (con apoyo del Repository):** La regla de solapamiento exige contrastar contra reservas de otros usuarios ya confirmadas en el mismo laboratorio físico. No es posible validarla en aislamiento sin consultar la base de datos, justificando plenamente el uso de `ReservaRepository.buscarSolapamientos()`.

---

### Punto de decisión 3 — Cómo comparten Service el Controller MVC y el REST
*(Pendiente de documentación en Parte 2)*

---

### Punto de decisión 4 — Manejo de errores consistente entre MVC y REST
*(Pendiente de documentación en Parte 2)*

---

## Herramientas utilizadas
- **Java 17** (LTS)
- **Spring Boot 3.2.0**
- **Spring Data JPA & Hibernate**
- **H2 Database Engine**
- **JUnit 5 & MockMvc**
- **Git & GitHub**