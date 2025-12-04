FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Запуск с минимальным образком
FROM openjdk:17.0.1-jdk-slim

# Уменьшаем образ: убираем кэш, документацию
RUN apt-get update && \
    apt-get purge -y --auto-remove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir -p /data

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-Ddata.path=/data", "-jar", "app.jar"]