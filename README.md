# El Volcán Market — Backend

Backend de la tienda online de **Distribuidora de Gas El Volcán** (DSY1104 Desarrollo FullStack II, Duoc UC — Evaluación 3).

Frontend (React): https://github.com/matiasfigueroaesc/elvolcanmarket

Sigue la estructura del proyecto de referencia del docente (GameTech EV3): Maven multi-módulo, un módulo `common` compartido, microservicios independientes y un API Gateway.

## Arquitectura

El frontend llama **solo al Gateway** (8080), que reenvía cada petición al microservicio que corresponde. Cada microservicio tiene su propia base de datos. No hay relaciones JPA ni acceso a tablas entre servicios: se comunican por HTTP.

| Módulo | Puerto | Base propia | Responsabilidad | Responsable | Estado |
|---|---:|---|---|---|---|
| `gateway` | 8080 | — | Rutas, CORS y Swagger agregado | Matías | ✅ |
| `common` | — | — | Biblioteca compartida: errores JSON, seguridad JWT, OpenAPI (**no** es microservicio) | Matías | ✅ |
| `usuarios` | 8081 | `volcan_usuarios` | Registro, login, JWT, refresh y CRUD de usuarios | Renato | ✅ |
| `catalogo` | 8082 | `volcan_catalogo` | Productos, categorías, ofertas y stock | Matías | ✅ |
| `ventas` | 8083 | `volcan_ventas` | Boletas y detalle de boleta | Fabián | Esqueleto |
| `despacho` | 8084 | `volcan_despacho` | Estado de los pedidos y zonas | Fabián | Esqueleto |

Modelo de datos de la pauta: `USUARIO` (usuarios), `CATEGORIA` y `PRODUCTO` (catalogo), `BOLETA` y `DETALLE_BOLETA` (ventas).

### Capas de cada microservicio

```
catalogo/src/main/java/cl/elvolcan/catalogo/
├── Application.java   ← arranque (encima de las capas para que Spring encuentre todo)
├── config/            ← SecurityConfig (permisos por rol), Seed (datos iniciales)
├── controller/        ← endpoints REST: reciben HTTP y delegan en el servicio
├── dto/               ← records de entrada (con validaciones) y de salida
├── exception/         ← GlobalExceptionHandler (@ControllerAdvice)
├── model/             ← entidades JPA (@Entity)
├── repository/        ← interfaces JpaRepository
└── service/           ← reglas de negocio y transacciones
```

Recorrido de una petición: **controller → service → repository → base de datos**, y la respuesta vuelve como DTO.

## Stack

- Java 17 (se recomienda tener instalado JDK 21) · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 · Maven Wrapper
- Spring Data JPA · H2 de archivo (por defecto) o MySQL 8.4
- Spring Security como *resource server* JWT (RS256) · BCrypt
- springdoc-openapi (Swagger) · JUnit 5 + MockMvc
- Sin Lombok: los DTO son `record` de Java y las entidades son clases simples.

## Requisitos

- **JDK 17 o superior** (`java -version`)
- No hace falta instalar Maven: se usa `mvnw` / `mvnw.cmd`
- Internet la primera vez (descarga de dependencias)
- Docker Desktop para levantar todo con MySQL (forma de presentación y de AWS)

## Levantar todo con Docker (MySQL + microservicios + frontend)

Es la forma de presentación y la misma que se usa en AWS. Los dos repos deben estar uno al lado del otro:

```
proyectos/
├── elvolcanmarket/           (frontend React)
└── elvolcanmarket-backend/   (este repo)
```

```bash
cp .env.example .env                           # Windows: copy .env.example .env
docker compose --profile web up -d --build     # la primera vez tarda ~10 min
docker compose --profile web ps                # mysql "healthy", el resto "running"
```

- Tienda: http://localhost
- Swagger: http://localhost/swagger-ui.html (o http://localhost:8080/swagger-ui.html)

Sin `--profile web` se levanta solo el backend (Gateway en :8080), por ejemplo para usar el front con `npm run dev`.
Cada microservicio corre con el perfil `mysql` y su propia base. Ver `Dockerfile` y `docker-compose.yml`.

**Despliegue en AWS (Learner Lab, una EC2):** [docs/DESPLIEGUE_AWS.md](docs/DESPLIEGUE_AWS.md).

## Compilar y probar

Desde la raíz del repo:

```bash
./mvnw verify          # Linux / macOS / Git Bash
mvnw.cmd verify        # Windows (cmd o PowerShell)
```

Compila los 6 módulos y ejecuta todas las pruebas. Los `.jar` quedan en `<modulo>/target/`.

## Ejecutar (perfil H2, sin instalar nada)

Después de `verify`, cada servicio en su propia terminal, desde la raíz:

```bash
java -jar catalogo/target/catalogo-1.0.0.jar
java -jar gateway/target/gateway-1.0.0.jar
```

También se puede ejecutar `Application.java` de cada módulo con el botón ▶ de IntelliJ o VS Code.

La base H2 queda en `data/` dentro de la carpeta desde donde se ejecuta, y conserva los datos entre reinicios. Para partir de cero, borra esa carpeta.

Prueba rápida, con catálogo y gateway arriba:

- http://localhost:8080/api/v1/productos — catálogo a través del Gateway
- http://localhost:8082/actuator/health — salud del catálogo
- http://localhost:8080/swagger-ui.html — Swagger (elegir el servicio arriba a la derecha)

## Ejecutar con MySQL (perfil `mysql`)

1. Crear las bases y usuarios, **una de dos**:
   - **Sin Docker:** ejecutar `sql/01_crear_bases_mysql.sql` como root en MySQL Workbench.
   - **Con Docker (solo la base):** `cp .env.example .env` (Windows: `copy`) y `docker compose up -d mysql`.
2. Iniciar cada servicio con el perfil:
   ```bash
   java -jar catalogo/target/catalogo-1.0.0.jar --spring.profiles.active=mysql
   ```

Cada servicio se conecta con su propio usuario (`volcan_catalogo`, etc.), que solo tiene permisos sobre su base. La contraseña por defecto es `volcan123` (variable `DB_PASSWORD`). Las tablas las crea JPA al arrancar y los datos iniciales los carga `Seed.java`.

| Docker | Comando |
|---|---|
| Detener (mantiene los datos) | `docker compose --profile web stop` |
| Consola SQL | `docker exec -it volcan-mysql mysql -u volcan_catalogo -p` |
| Borrar todo y partir de cero | `docker compose --profile web down -v` |

## API del catálogo

Base: `http://localhost:8080/api/v1`. Todas las respuestas son JSON, incluidos los errores:
`{ "timestamp": "...", "status": 400, "mensaje": "Revisa los campos del formulario.", "campos": { "precio": "El precio debe ser mayor a 0." } }`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/productos?texto=&categoriaId=&oferta=` | Público | Catálogo con filtros opcionales |
| GET | `/productos/{id}` | Público | Detalle |
| GET | `/productos/criticos` | ADMIN, VENDEDOR | Stock ≤ stock crítico |
| POST | `/productos` | ADMIN | Crear (201) |
| PUT | `/productos/{id}` | ADMIN | Editar |
| DELETE | `/productos/{id}` | ADMIN | Eliminar (borrado lógico, 204) |
| GET | `/categorias` | Público | Lista |
| GET | `/categorias/{id}` | Público | Detalle |
| GET | `/categorias/slug/{slug}` | Público | Para `/categorias/:slug` de la tienda |
| POST | `/categorias` | ADMIN | Crear (el slug se genera solo) |
| PUT | `/categorias/{id}` | ADMIN | Editar |
| DELETE | `/categorias/{id}` | ADMIN | Eliminar (409 si tiene productos) |

Los nombres de los campos son los mismos que usa el frontend (`codigo`, `precioOferta`, `stockCritico`, `categoriaId`, `unidad`…), así los componentes de React no cambian.

## API de usuarios

Base: `http://localhost:8080/api/v1`. Los campos son los mismos de `src/data/usuarios.js` del frontend (`run`, `nombre`, `apellidos`, `correo`, `telefono`, `fechaNacimiento`, `tipo`, `region`, `comuna`, `direccion`). La respuesta **nunca** trae la contraseña.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público* | `{ "correo", "password" }` → `{ accessToken, expiresIn, usuario }` + cookie `ev_refresh` |
| POST | `/auth/registro` | Público | Crea siempre un `cliente` (201). 409 si el correo o el RUN ya existen |
| POST | `/auth/refresh` | Público* | Usa la cookie, entrega un JWT nuevo y rota el refresh |
| POST | `/auth/logout` | Público* | Invalida el refresh y borra la cookie (204) |
| GET | `/auth/me` | Con sesión | Datos del usuario del token |
| GET | `/auth/jwks` | Público | Clave pública RSA para validar el JWT |
| GET | `/usuarios?texto=&tipo=` | ADMIN | Lista con filtros opcionales |
| GET | `/usuarios/{id}` | ADMIN | Detalle |
| POST | `/usuarios` | ADMIN | Crear de cualquier tipo (201) |
| PUT | `/usuarios/{id}` | ADMIN | Editar. `password` vacío = no cambiarla. El RUN no cambia |
| DELETE | `/usuarios/{id}` | ADMIN | Borrado lógico (204). No se puede eliminar la propia cuenta ni el último administrador |

\* Login, refresh y logout exigen el encabezado `X-Requested-With: ElVolcanMarket` (protección CSRF, porque usan cookie). Desde React con `fetch` hay que enviar también `credentials: "include"` para que viaje la cookie.

`tipo` del frontend → rol del JWT: `administrador` → `ADMIN`, `vendedor` → `VENDEDOR`, `cliente` → `CLIENTE`.

Usuarios de prueba (los mismos del frontend): `javiera.munoz@gmail.com` / `admin1234` (administrador), `pedro.salinas@duoc.cl` / `vendedor1234` (vendedor), `camila.toro@gmail.com` / `cliente1234` (cliente).

Las claves RSA se generan solas la primera vez en `data/keys/` (no se suben a Git). Si las borras, los tokens emitidos antes dejan de servir y hay que volver a iniciar sesión.

## Contratos entre servicios (para usuarios y ventas)

**Token JWT (lo emite `usuarios`, lo validan todos con `common/SecuritySupport`):**
- Firmado con **RS256**; la clave pública se publica en `GET /api/v1/auth/jwks` (servicio usuarios).
- `iss` = `elvolcan-usuarios`, `aud` = `elvolcan-api`.
- Claim `roles` = `["ADMIN"]`, `["VENDEDOR"]` o `["CLIENTE"]`.
- Access token de 15 minutos + refresh token en cookie HttpOnly (igual que el proyecto del profe).

**Descuento de stock (ventas → catálogo):**
- `POST http://localhost:8082/internal/v1/ventas/{boletaId}` con el encabezado `X-Internal-Key` (variable `INTERNAL_API_KEY`).
- Cuerpo: `[{ "id": 4, "cantidad": 2 }, ...]`.
- Respuesta: `{ "lineas": [{ "id", "codigo", "nombre", "precio", "cantidad", "imagen" }], "total": 79980 }`. El **precio lo fija el catálogo** (aplica la oferta); ventas copia estas líneas en `DETALLE_BOLETA`.
- Es todo o nada (409 si falta stock y no se descuenta nada) e idempotente (repetir la misma boleta no descuenta dos veces).
- No pasa por el Gateway.

**Rutas que ya enruta el Gateway:** `/api/v1/auth/**` y `/api/v1/usuarios/**` → usuarios; `/api/v1/productos/**` y `/api/v1/categorias/**` → catalogo; `/api/v1/boletas/**` → ventas; `/api/v1/despachos/**` y `/api/v1/zonas/**` → despacho.

## Flujo de trabajo

- `main`: versión entregable.
- `develop`: integración del equipo.
- Cada integrante trabaja en `feature/<nombre>-<tarea>`, abre un PR hacia `develop` y solo trabaja dentro de su módulo.
- Antes de abrir el PR: `mvnw verify` en verde.
