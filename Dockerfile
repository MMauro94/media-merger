FROM gradle:7-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle jar --no-daemon -PjarName=app.jar


FROM openjdk:17-alpine

WORKDIR /media
VOLUME /media

RUN apk add \
    mkvtoolnix \
    ffmpeg

COPY --from=build /home/gradle/src/build/libs/app.jar /app/app.jar


ENTRYPOINT ["java", "-jar", "/app/app.jar"]