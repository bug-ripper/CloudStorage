FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY . .

RUN mvn -pl AuthService -am clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/AuthService/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]