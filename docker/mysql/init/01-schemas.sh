#!/bin/bash
# Crea un schema por microservicio y le da permisos al usuario de la app.
# MySQL lo ejecuta solo la primera vez que se crea el volumen.
# Las TABLAS no se crean aquí: las crea cada microservicio con JPA.
set -e

SCHEMAS="volcan_auth volcan_usuarios volcan_catalogo volcan_inventario volcan_ventas volcan_despacho"

for schema in $SCHEMAS; do
  echo "Creando schema ${schema}..."
  mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${schema}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON \`${schema}\`.* TO '${MYSQL_USER}'@'%';
SQL
done

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "FLUSH PRIVILEGES;"
echo "Schemas listos."
