FROM gradle:jdk11-alpine as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build shadowJar

FROM openjdk:11-jre
COPY --from=builder /home/gradle/src/build/libs/oeuv-bot-1.0-all.jar /app/oeuv-bot.jar
WORKDIR /app
CMD ["java", "-jar", "oeuv-bot.jar"]