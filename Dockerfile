FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY data ./data
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV PORT=10000

COPY --from=build /app/target/linebot-english-learning-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 10000

CMD ["sh", "-c", "java -jar /app/app.jar"]