# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime ligero optimizado para instancias EC2 (t2.micro / t3.small)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Límite estricto de memoria heap para evitar congelamiento de la EC2 (OOM)
ENV JAVA_OPTS="-Xms128m -Xmx256m -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
