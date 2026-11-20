-- El Volcán Market — bases para el perfil "mysql" SIN Docker.
-- Ejecutar como root (MySQL Workbench o consola). Hace lo mismo que
-- docker/mysql/init/01-schemas.sh. Las tablas las crea cada microservicio (JPA).
-- La contraseña volcan123 es la que usan por defecto los application-mysql.yml.

CREATE DATABASE IF NOT EXISTS volcan_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS volcan_catalogo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS volcan_ventas   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS volcan_despacho CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'volcan_usuarios'@'%' IDENTIFIED BY 'volcan123';
CREATE USER IF NOT EXISTS 'volcan_catalogo'@'%' IDENTIFIED BY 'volcan123';
CREATE USER IF NOT EXISTS 'volcan_ventas'@'%'   IDENTIFIED BY 'volcan123';
CREATE USER IF NOT EXISTS 'volcan_despacho'@'%' IDENTIFIED BY 'volcan123';

-- Cada usuario solo puede tocar SU base.
GRANT ALL PRIVILEGES ON volcan_usuarios.* TO 'volcan_usuarios'@'%';
GRANT ALL PRIVILEGES ON volcan_catalogo.* TO 'volcan_catalogo'@'%';
GRANT ALL PRIVILEGES ON volcan_ventas.*   TO 'volcan_ventas'@'%';
GRANT ALL PRIVILEGES ON volcan_despacho.* TO 'volcan_despacho'@'%';

FLUSH PRIVILEGES;
