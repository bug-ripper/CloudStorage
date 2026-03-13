FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY . .

RUN mvn -pl MetadataService -am clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/MetadataService/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","app.jar"]