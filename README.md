# Post-contenido — Unidad 5: Integración en Aplicaciones Web (ReservasLabs)

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`reservas-labs-api`) para la gestión y reserva de laboratorios de cómputo universitarios estructurado en dos partes: una API REST en capas sobre base de datos H2 embebida y una vista web MVC clásico con plantillas Thymeleaf que reutiliza exactamente la misma capa de servicio.

---

## Cómo ejecutar

### Prerrequisitos
- **Java 17** o superior instalado y configurado en el `PATH`.
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
- **API REST (JSON):**
  - Laboratorios: `http://localhost:8080/api/laboratorios`
  - Reservas: `http://localhost:8080/api/reservas`
- **Vista Web MVC (Thymeleaf):**
  - Listado de reservas: `http://localhost:8080/reservas`
  - Formulario de nueva reserva: `http://localhost:8080/reservas/nueva`
- **Consola H2:**
  - URL: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:reservas_labs_db`
  - Usuario: `sa` / Contraseña: (vacía)

---

## Estructura del Proyecto

```text
gallo-post1-u5/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.properties
    │   │   └── templates/reservas/
    │   │       ├── lista.html
    │   │       └── nueva.html
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
    │       ├── controller/
    │       │   ├── LaboratorioController.java
    │       │   └── ReservaController.java
    │       └── web/
    │           ├── ReservaWebController.java
    │           └── ReservaWebExceptionHandler.java
    └── test/java/com/universidad/reservaslabs/
        ├── ReservaApiIntegrationTest.java
        └── ReservaMvcIntegrationTest.java
```

---

## Decisiones de diseño

### Punto de decisión 1 — Ubicación de la validación de solapamiento de horarios
* **Decisión arquitectónica:** La verificación de solapamiento delega el filtrado en una consulta JPQL explícita (`ReservaRepository.buscarSolapamientos()`), mientras que la decisión de negocio (lanzar `ReservaConflictException` con código 409) vive exclusivamente en `ReservaService`.
* **Justificación técnica:**
  1. **Eficiencia y escalabilidad:** Validar en memoria con Java puro traería todas las reservas del laboratorio a la JVM (`findAll()`), consumiendo memoria y degradando el rendimiento conforme el volumen de reservas crece a cientos o miles. La consulta JPQL ejecuta el filtrado en el motor de base de datos SQL (`inicio < :fin AND fin > :inicio AND estado <> CANCELADA`).
  2. **Separación de responsabilidades:** El Repository responde a una pregunta de datos pura (*"¿qué reservas activas colisionan con este rango?"*), mientras que el Service decide qué significa ese resultado para el negocio (*"si hay reservas, se cancela la operación y se arroja un error 409 Conflict"*). Si el controlador llamara directamente al Repository, la regla de negocio quedaría secuestrada en la capa web, violando la arquitectura en capas.

---

### Punto de decisión 2 — Reglas con y sin apoyo del Repository
* **Decisión arquitectónica:** Distinguir entre reglas intrínsecas a la entidad y reglas relacionales:
  1. **Reglas puras de dominio (sin apoyo del Repository):** La validación de horario de atención institucional (07:00 a 21:00) y de duración permitida (mínimo 30 minutos, máximo 3 horas) depende exclusivamente de los atributos propios de la reserva que se intenta registrar (`inicio` y `fin`). Se ejecuta en `ReservaService.validarHorarioYDuracion()` con Java puro (`java.time.LocalTime` y `java.time.Duration`) antes de cualquier acceso a la base de datos.
  2. **Reglas relacionales (con apoyo del Repository):** La regla de solapamiento exige contrastar contra reservas de otros usuarios ya confirmadas en el mismo laboratorio físico. No es posible validarla en aislamiento sin consultar la base de datos, justificando plenamente el uso de `ReservaRepository.buscarSolapamientos()`.

---

### Punto de decisión 3 — Cómo comparten Service el Controller MVC y el REST
* **Decisión arquitectónica:** Tanto `ReservaController` (REST) como `ReservaWebController` (MVC Thymeleaf) reciben por inyección de dependencias la **misma instancia Singleton de `ReservaService`**, gestionada por el contenedor de Spring.
* **Justificación técnica:**
  - Se descarta crear un segundo servicio (`ReservaWebService`) o duplicar código de validación en el controlador web.
  - Al compartir el mismo Service, cualquier cambio futuro en las reglas de solapamiento, duraciones permitidas u horarios de atención se propaga automáticamente a ambas superficies (API móvil/externa y vistas web de escritorio) sin discrepancias de comportamiento ni código repetido.

---

### Punto de decisión 4 — Manejo de errores consistente entre MVC y REST
* **Decisión arquitectónica:** Se crearon dos manejadores de excepciones desacoplados y especializados por superficie:
  1. `GlobalRestExceptionHandler`: `@RestControllerAdvice(annotations = RestController.class)` $\rightarrow$ Serializa las excepciones a respuestas JSON estructuradas con códigos HTTP (409 Conflict para solapamientos y horarios inválidos, 404 Not Found para recursos inexistentes, 400 Bad Request para validaciones de formulario).
  2. `ReservaWebExceptionHandler`: `@ControllerAdvice(assignableTypes = ReservaWebController.class)` $\rightarrow$ Captura las mismas excepciones de dominio pero redirige al formulario web (`/reservas/nueva`) inyectando el mensaje de error legible vía `RedirectAttributes` flash.
* **Justificación técnica:**
  - Se descartó un único manejador con condicionales para detectar la cabecera `Accept` del cliente, ya que generaría métodos complejos con ramas if/else propensas a errores.
  - La separación por anotaciones (`annotations` y `assignableTypes`) garantiza que ambas capas compartan el mismo vocabulario de excepciones de dominio (`ReservaConflictException`, `RecursoNoEncontradoException`) pero entreguen la representación idónea para su canal (JSON estructurado vs. vista HTML renderizada).

---

## Herramientas utilizadas
- **Java 17** (LTS)
- **Spring Boot 3.2.0**
- **Spring Web (REST) & Spring MVC**
- **Spring Data JPA & Hibernate**
- **Thymeleaf**
- **H2 Database Engine**
- **JUnit 5 & MockMvc**
- **Git & GitHub**

---

## Conclusiones
La implementación de este laboratorio evidenció que una arquitectura en capas sólida trasciende la mera organización de paquetes: garantiza que las reglas de negocio críticas residan en el Service y no queden diluidas en los controladores ni reducidas a un passthrough anémico. La reutilización exitosa del mismo `ReservaService` tanto para la API REST como para las vistas MVC con Thymeleaf demostró cómo un núcleo de dominio bien diseñado desacopla completamente la lógica de negocio de su mecanismo de presentación, manteniendo una experiencia consistente y robusta ante errores en todas las superficies del sistema.