# Imagen de UN microservicio (el mismo archivo sirve para todos, como en el ejemplo del docente).
# Se elige el módulo con el argumento MODULE: usuarios, catalogo, ventas, despacho o gateway.
#   docker build --build-arg MODULE=catalogo -t elvolcan-catalogo .
# Normalmente no se usa a mano: docker compose lo construye para cada servicio.

# ---------- Etapa 1: compilar con Maven (JDK completo) ----------
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
# Primero solo los pom.xml: Docker guarda en caché las dependencias descargadas
# y no las vuelve a bajar si solo cambia el código. Es una optimización: si algo no se
# puede bajar aquí (por ejemplo, el módulo common, que es nuestro), se baja en el paso siguiente.
COPY pom.xml .
COPY common/pom.xml common/
COPY usuarios/pom.xml usuarios/
COPY catalogo/pom.xml catalogo/
COPY ventas/pom.xml ventas/
COPY despacho/pom.xml despacho/
COPY gateway/pom.xml gateway/
RUN mvn -B -q dependency:go-offline -DexcludeArtifactIds=common || true
# Luego el código. Se compilan todos los módulos una sola vez (las pruebas se corren
# aparte con mvnw verify); la caché hace que los demás servicios reutilicen este paso.
COPY . .
RUN mvn -B -DskipTests package

# ---------- Etapa 2: solo lo necesario para ejecutar (JRE, más liviano) ----------
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
# Carpeta para datos del servicio (las claves RSA del JWT en usuarios).
# Se ejecuta con un usuario sin privilegios, no como root.
RUN mkdir /app/data && chown 10001:10001 /app/data
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8080 8081 8082 8083 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
