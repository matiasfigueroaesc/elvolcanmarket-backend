# Despliegue en AWS (Learner Lab) — El Volcán Market

Todo el sistema corre en **una máquina EC2** con **Docker Compose**: MySQL, los 4 microservicios,
el Gateway y el frontend (nginx). Es el mismo `docker-compose.yml` que se usa en local.

```
Internet ──:80──▶ frontend (nginx) ──/api, Swagger──▶ gateway :8080 ──▶ usuarios :8081
                     │                                                  ├─▶ catalogo :8082
                     └─ build de React                                  ├─▶ ventas   :8083
                                                                        └─▶ despacho :8084
                                                       todos ──▶ mysql (volcan_usuarios, volcan_catalogo, ...)
```

Solo el puerto **80** queda abierto a internet. Los microservicios y MySQL solo se ven dentro de la
red interna de Docker.

---

## 1. Iniciar el laboratorio

1. Entra a AWS Academy → tu curso → **Modules → Learner Lab**.
2. Pulsa **Start Lab** y espera a que el círculo junto a "AWS" quede **verde** (1-3 minutos).
3. Pulsa **AWS** para abrir la consola. Región: **N. Virginia (us-east-1)**.

> La sesión dura 4 horas. Al terminar, AWS **detiene** la EC2 (no la borra) y al volver a iniciar
> el lab se puede encender de nuevo. Los datos de MySQL se conservan.

## 2. Crear la máquina (EC2)

Consola → **EC2 → Launch instance**:

| Campo | Valor |
|---|---|
| Name | `elvolcan-market` |
| AMI | **Ubuntu Server 24.04 LTS** (x86_64) |
| Instance type | **t3.medium** (2 vCPU, 4 GB). Con menos memoria la compilación de Java falla |
| Key pair | `vockey` (la del laboratorio) |
| Network settings → Firewall | *Create security group* y marca: **Allow SSH** (desde *My IP*) y **Allow HTTP traffic from the internet** |
| Storage | **30 GiB** gp3 |

Pulsa **Launch instance** y espera a que el estado sea *Running* con "2/2 checks passed".

### (Recomendado) IP fija: Elastic IP

Cada vez que la EC2 se detiene y vuelve a encender, AWS le cambia la IP pública. Para que la
dirección no cambie entre ensayos y la presentación:

EC2 → **Elastic IPs → Allocate Elastic IP address → Allocate**. Luego **Actions → Associate**,
elige la instancia `elvolcan-market` y pulsa **Associate**.

## 3. Conectarse

EC2 → selecciona la instancia → **Connect → EC2 Instance Connect → Connect**. Se abre una
terminal en el navegador, con el usuario `ubuntu`.

## 4. Instalar Docker (una sola vez)

```bash
# Docker Engine + Compose (script oficial de Docker)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker            # aplica el grupo sin cerrar la sesión
docker compose version   # debe mostrar "Docker Compose version v2..."

# Memoria de respaldo (swap) de 2 GB: evita que la compilación se quede sin memoria
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 5. Bajar el proyecto

Los dos repos van **uno al lado del otro** (el compose busca el frontend en `../elvolcanmarket`):

```bash
mkdir -p ~/proyectos && cd ~/proyectos
git clone https://github.com/matiasfigueroaesc/elvolcanmarket-backend.git
git clone https://github.com/matiasfigueroaesc/elvolcanmarket.git
```

Usa las ramas que se van a presentar (por ejemplo `main`, o `develop`/`eval3` antes de la entrega):

```bash
cd ~/proyectos/elvolcanmarket && git checkout main
cd ~/proyectos/elvolcanmarket-backend && git checkout main
```

## 6. Configurar el `.env`

```bash
cd ~/proyectos/elvolcanmarket-backend
cp .env.example .env
openssl rand -hex 24      # copia el resultado para INTERNAL_API_KEY
nano .env
```

Cambia como mínimo `MYSQL_ROOT_PASSWORD`, `MYSQL_APP_PASSWORD` e `INTERNAL_API_KEY`
(guardar: `Ctrl+O`, `Enter`; salir: `Ctrl+X`). No hace falta poner la IP en ningún lado.

## 7. Levantar todo

```bash
docker compose --profile web up -d --build
```

La primera vez tarda **10 a 15 minutos**, porque descarga las imágenes y compila Java y React.
Después:

```bash
docker compose ps                    # todos "running"; mysql "healthy"
docker compose logs -f usuarios      # ver cómo arranca un servicio (Ctrl+C para salir)
```

Abre en el navegador **`http://<IP pública de la EC2>`**. La IP aparece en EC2 → Instances →
*Public IPv4 address*. Usa **http**, no https.

- Tienda: `http://<IP>/`
- Panel: `http://<IP>/admin` (`javiera.munoz@gmail.com` / la `ADMIN_PASSWORD` del `.env`)
- Swagger: `http://<IP>/swagger-ui.html`

## 8. Comandos del día a día

| Acción | Comando (en `~/proyectos/elvolcanmarket-backend`) |
|---|---|
| Ver estado | `docker compose --profile web ps` |
| Ver logs | `docker compose logs -f gateway` |
| Actualizar a lo último de GitHub | `git pull && (cd ../elvolcanmarket && git pull) && docker compose --profile web up -d --build` |
| Reiniciar un servicio | `docker compose restart catalogo` |
| Detener todo (conserva los datos) | `docker compose --profile web stop` |
| **Borrar todo, incluida la base** | `docker compose --profile web down -v` |

Los contenedores tienen `restart: unless-stopped`: cuando la EC2 se vuelve a encender, se levantan
solos.

## 9. Antes de la presentación

1. Inicia el Learner Lab **30 minutos antes** y enciende la EC2 si está detenida
   (EC2 → Instance state → **Start**).
2. Espera unos 2 minutos y revisa `docker compose --profile web ps`.
3. Abre la tienda y el panel, e inicia sesión con los tres roles.
4. Si no usaste Elastic IP, anota la **IP nueva**.

## 10. Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| La página no carga (se queda esperando) | Falta abrir el puerto 80 en el security group, o usaste `https://` | EC2 → Security → Security group → Edit inbound rules → HTTP 80 desde 0.0.0.0/0. Usa `http://` |
| `502 Bad Gateway` al entrar | El Gateway o un servicio todavía está arrancando | Espera 1-2 minutos. `docker compose logs gateway` |
| `Falta INTERNAL_API_KEY en el archivo .env` | No existe el `.env` | Paso 6 |
| La compilación se corta (`Killed`, `exit code 137`) | Sin memoria | Usa t3.medium y crea el swap (paso 4) |
| Un servicio se reinicia en bucle con un error de MySQL | Cambiaste las contraseñas del `.env` después de crear la base | `docker compose --profile web down -v` y volver a levantar (borra los datos) |
| `unable to prepare context: path "../elvolcanmarket" not found` | El repo del front no está al lado del backend | Paso 5, o ajusta `FRONTEND_DIR` en el `.env` |
| Se cerró la sesión del lab | La EC2 se detuvo | Start Lab → EC2 → Start. Los datos siguen ahí |

## 11. Costo

Learner Lab entrega un crédito limitado. Una t3.medium cuesta del orden de USD 0,04 por hora
encendida. **Detén la instancia** (Instance state → Stop) cuando no la estén usando. Una Elastic IP
asociada a una instancia detenida tiene un costo mínimo por hora.
