#!/bin/bash
# Crea una base y un usuario por microservicio (como el ejemplo del profe:
# cada servicio solo puede tocar SU base).
# MySQL lo ejecuta solo la primera vez que se crea el volumen.
# Las TABLAS no se crean aquí: las crea cada microservicio con JPA.
set -e

SERVICIOS="usuarios catalogo ventas despacho"

for s in $SERVICIOS; do
  echo "Creando base y usuario volcan_${s}..."
  mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`volcan_${s}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'volcan_${s}'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON \`volcan_${s}\`.* TO 'volcan_${s}'@'%';
SQL
done

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "FLUSH PRIVILEGES;"
echo "Bases listas."
