# ===========================
#   STAGE 1 : Build with Maven
# ===========================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar POM y descargar dependencias primero (cache)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copiar el código fuente
COPY src ./src

# Construir la aplicación
RUN mvn -q clean package -DskipTests

# ===========================
#   STAGE 2 : Run App
# ===========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos el .jar desde el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Render asigna dinámicamente el puerto. Spring Boot debe leerlo.
ENV PORT=8080

# Spring Boot debe usar el port de Render:
#   server.port=${PORT}
ENTRYPOINT ["sh", "-c", "java -jar -Dserver.port=${PORT} app.jar"]
