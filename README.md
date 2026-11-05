# El Volcán Market — Backend

Backend de la tienda online de **Distribuidora de Gas El Volcán** (DSY1104 Desarrollo FullStack II, Duoc UC — Evaluación 3).

Frontend (React): https://github.com/matiasfigueroaesc/elvolcanmarket

## Arquitectura

El frontend llama solo al **API Gateway**, que reparte cada petición al microservicio que corresponde. Cada microservicio es un proyecto Spring Boot independiente con su propio schema en MySQL.

| Servicio | Puerto | Schema MySQL | Responsabilidad |
|---|---|---|---|
| `api-gateway` | 8080 | — | Punto de entrada único, CORS, validación del JWT |
| `ms-auth` | 8081 | `volcan_auth` | Login y emisión del JWT |
| `ms-usuarios` | 8082 | `volcan_usuarios` | Usuarios, registro y perfil |
| `ms-catalogo` | 8083 | `volcan_catalogo` | Productos, categorías y ofertas |
| `ms-inventario` | 8084 | `volcan_inventario` | Stock y productos críticos |
| `ms-ventas` | 8085 | `volcan_ventas` | Boletas y detalle de boleta |
| `ms-despacho` | 8086 | `volcan_despacho` | Estado del despacho y zonas |

Todas las rutas siguen el formato `/api/v1/<recurso>` y responden en JSON (incluidos los errores).

## Stack

- Java 21 · Spring Boot 3 · Maven
- Spring Data JPA · MySQL 8.4 (Docker)
- Spring Security + JWT (8 h) · contraseñas con BCrypt
- springdoc-openapi (Swagger) · Lombok
- JUnit 5 + Mockito

Roles: `administrador`, `vendedor` (solo lectura de productos y órdenes) y `cliente` (solo tienda).

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- JDK 21
- Un IDE (IntelliJ IDEA, VS Code o Spring Tool Suite)

## Levantar la base de datos

```bash
cp .env.example .env        # en Windows (cmd): copy .env.example .env
docker compose up -d
docker compose ps           # mysql debe aparecer como "healthy"
```

Se crea un contenedor `volcan-mysql` con los 6 schemas y el usuario de la aplicación (definido en `.env`). Las **tablas** las crea cada microservicio con JPA al arrancar, y los datos iniciales se cargan desde el `data.sql` de cada uno.

Conexión desde un cliente (DBeaver, MySQL Workbench): `localhost:3306`, usuario y contraseña del `.env`.

### Comandos útiles

| Acción | Comando |
|---|---|
| Detener (mantiene los datos) | `docker compose stop` |
| Ver logs de MySQL | `docker compose logs -f mysql` |
| Consola SQL | `docker exec -it volcan-mysql mysql -u volcan -p` |
| **Borrar todo y partir de cero** | `docker compose down -v` |

> Si cambias el script de `docker/mysql/init/`, tienes que borrar el volumen (`down -v`), porque solo se ejecuta la primera vez.

## Estructura del repositorio

```
elvolcanmarket-backend/
├── docker-compose.yml
├── docker/mysql/init/   # crea los schemas
├── api-gateway/         # (próximamente)
├── ms-auth/
├── ms-usuarios/
├── ms-catalogo/
├── ms-inventario/
├── ms-ventas/
└── ms-despacho/
```

Cada carpeta `ms-*` es un proyecto Maven independiente, paquete base `cl.elvolcan.<servicio>`.

## Flujo de trabajo

- `main`: versión entregable.
- `develop`: integración del equipo.
- Cada integrante trabaja en `feature/<nombre>-<tarea>` y abre un PR hacia `develop`.

## Equipo

| Integrante | Servicios |
|---|---|
| Matías Figueroa | api-gateway, ms-catalogo, ms-inventario, infraestructura e integración con el frontend |
| Renato | ms-auth, ms-usuarios |
| Fabián Rubio | ms-ventas, ms-despacho |
