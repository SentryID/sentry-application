# Imagem de um módulo da aplicação: docker build --build-arg MODULE=sentry-auth --build-arg PORT=8081 .
ARG MODULE=sentry-auth

FROM maven:3-eclipse-temurin-27 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/${MODULE}-*.jar /app.jar

# runtime só com JRE, sem Maven nem JDK (RN-F0-04.2)
FROM eclipse-temurin:27-jre-alpine
ARG PORT=8081
RUN addgroup -S sentry && adduser -S -G sentry sentry
COPY --from=build /app.jar /app/app.jar
USER sentry
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
