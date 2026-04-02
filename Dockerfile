FROM eclipse-temurin:17-jdk
COPY build/libs/five-bingo-SNAPSHOT.jar app.jar
LABEL authors="nowij"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]