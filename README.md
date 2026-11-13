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
| `ventas` | 8083 | `volcan_ventas` | Boletas y detalle de boleta | Fabián | ✅ |
| `despacho` | 8084 | `volcan_despacho` | Estado de los pedidos y zonas | Fabián | ✅ |

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
- MySQL o Docker **solo** si quieres usar el perfil `mysql`

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
   - **Con Docker:** `cp .env.example .env` (Windows: `copy`) y `docker compose up -d`.
2. Iniciar cada servicio con el perfil:
   ```bash
   java -jar catalogo/target/catalogo-1.0.0.jar --spring.profiles.active=mysql
   ```

Cada servicio se conecta con su propio usuario (`volcan_catalogo`, etc.), que solo tiene permisos sobre su base. La contraseña por defecto es `volcan123` (variable `DB_PASSWORD`). Las tablas las crea JPA al arrancar y los datos iniciales los carga `Seed.java`.

| Docker | Comando |
|---|---|
| Detener (mantiene los datos) | `docker compose stop` |
| Consola SQL | `docker exec -it volcan-mysql mysql -u volcan_catalogo -p` |
| Borrar todo y partir de cero | `docker compose down -v` |

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

## API de ventas

Base: `http://localhost:8080/api/v1`. Una boleta tiene la **misma forma que una orden de `src/data/ordenes.js`** (`id`, `numero`, `fecha`, `usuarioId`, `cliente`, `direccion`, `entrega`, `items`, `total`, `estadoPago`, `estado`), así `OrderSummary`, `OrderTable` y `Boleta.jsx` no cambian. `estado` es el estado del despacho (lo informa el servicio despacho); es `null` si el pago fue rechazado.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/boletas` | Público | Checkout. Con sesión queda a nombre del usuario del token; sin sesión, como invitado (`usuarioId: null`). 201 también si el pago es rechazado |
| GET | `/boletas/mias` | Con sesión | Compras del usuario del token, más nuevas primero |
| GET | `/boletas?texto=&estadoPago=&usuarioId=` | ADMIN, VENDEDOR | Listado del panel. `usuarioId` sirve para el historial de compras de un usuario |
| GET | `/boletas/{id}` | Con sesión | ADMIN y VENDEDOR ven cualquiera; CLIENTE solo las suyas (si no, 404) |

Cuerpo del checkout (los campos de `CheckoutForm.jsx` agrupados como en `crearOrden`):

```json
{
  "cliente":   { "nombre": "Ana", "apellidos": "Rojas Díaz", "correo": "ana@gmail.com" },
  "direccion": { "calle": "Av. Libertad 123", "departamento": "", "region": "nuble",
                 "comuna": "chillan", "indicaciones": "" },
  "entrega": "express",
  "items": [{ "id": 4, "cantidad": 2 }],
  "simularRechazo": false
}
```

Reglas del checkout:
- **El precio y el total los pone el catálogo** (con la oferta aplicada). Si el carrito manda `precio` o `nombre`, se ignoran.
- **Pago aprobado:** se descuenta el stock (todo o nada). Si falta stock responde 409 con el mensaje del catálogo (ej. `"Stock insuficiente de Cilindro GLP 45 kg (disponible: 1)."`) y no queda ninguna boleta.
- **Pago rechazado** (`simularRechazo: true`): la boleta se guarda con `estadoPago: "rechazado"` para que el admin la vea, pero **no se descuenta stock** ni se crea despacho. El carrito no se vacía (lo maneja el front).
- El número `ORD-000001` sale del id de la boleta, igual que en el front.
- Como un invitado no puede volver a pedir su boleta por id, las páginas `/pago/exito/:id` y `/pago/error/:id` deberían usar la respuesta del POST (por ejemplo, pasarla en el `state` de `navigate`).

## API de despacho

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/zonas` | Público | Las zonas de `src/utils/zonasDespacho.js` (`id`, `nombre`, `cobertura[{comuna, sector}]`, `detalle`, `dias`, `horario`, `tiempo`) |
| GET | `/zonas/{id}` | Público | Detalle (ej. `/zonas/centro`) |
| GET | `/zonas/comuna/{comuna}` | Público | Igual que `zonasDeComuna()`: puede devolver 0, 1 o 2 zonas |
| POST / PUT / DELETE | `/zonas`, `/zonas/{id}` | ADMIN | Administrar zonas (el `id` se elige al crear y no cambia) |
| GET | `/despachos/seguimiento/{numero}` | Público | Para `/seguimiento`: `{ numero, estado, entrega, zona, tiempo, etapas, historial }`. **Sin datos personales** |
| GET | `/despachos?estado=` | ADMIN, VENDEDOR | Listado |
| GET | `/despachos/{boletaId}` | Con sesión | ADMIN y VENDEDOR cualquiera; CLIENTE solo el suyo |
| PATCH | `/despachos/{boletaId}/estado` | ADMIN | `{ "estado": "preparando" }`. Solo avanza: recibido → preparando → en camino → entregado (409 si se intenta volver atrás o ya está entregado). Cada cambio queda en `historial` con su fecha |

El despacho se identifica con el **id de la boleta**, así `/admin/ordenes/:id` sirve para los dos servicios. Al crearse se le asigna la zona que cubre su comuna (`zonaId`, `null` si no hay cobertura).

Datos de fábrica: las 5 zonas del front y las 2 órdenes de Camila (`ORD-000001` entregada y `ORD-000002` en camino), en ventas y en despacho con los mismos ids.

## Contratos entre servicios

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

**Creación del despacho (ventas → despacho):**
- `POST http://localhost:8084/internal/v1/despachos` con `X-Internal-Key` (misma `INTERNAL_API_KEY`).
- Cuerpo: `{ "boletaId": 3, "numero": "ORD-000003", "usuarioId": 3, "region": "nuble", "comuna": "chillan", "entrega": "normal" }`.
- Solo para boletas **pagadas**. Es idempotente: repetir la misma boleta devuelve el despacho que ya existe.
- Si despacho está caído, la venta igual se completa y ventas reintenta cada minuto (`REINTENTO_DESPACHO_MS`) hasta registrarlo.

**Estado de despacho de varias boletas (ventas → despacho):**
- `POST http://localhost:8084/internal/v1/despachos/estados` con `X-Internal-Key`. Cuerpo `[1, 2, 3]` → `{ "1": "entregado", "2": "en camino" }`.
- Ventas lo usa para completar el campo `estado` de sus respuestas. Si despacho no responde, las boletas se muestran igual, con `estado: null`.

**Rutas que ya enruta el Gateway:** `/api/v1/auth/**` y `/api/v1/usuarios/**` → usuarios; `/api/v1/productos/**` y `/api/v1/categorias/**` → catalogo; `/api/v1/boletas/**` → ventas; `/api/v1/despachos/**` y `/api/v1/zonas/**` → despacho.

## Flujo de trabajo

- `main`: versión entregable.
- `develop`: integración del equipo.
- Cada integrante trabaja en `feature/<nombre>-<tarea>`, abre un PR hacia `develop` y solo trabaja dentro de su módulo.
- Antes de abrir el PR: `mvnw verify` en verde.
