#!/bin/bash
FROM maven:3.8.7-openjdk-18 as build
LABEL maintainer="qusai"

LABEL service=payment \
      stage=build

RUN mkdir /ms
WORKDIR /ms
# Copy the source code to build it
COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Build the executable jar (Spring Boot fat jar)
RUN  mvn clean package -DskipTests=true \
  && mv ./target/payment*.jar ./ms.jar


# Deployment layer

FROM openjdk:18-slim
LABEL service=payment \
      stage=deploy


# Add a non-root user user an
RUN addgroup -gid 1009  upayment \
 && adduser -uid 1009 -gid 1009 --gecos "upayment upayment,upayment,upayment,upayment" --disabled-password upayment \
 &&  mkdir -p /ms && chown -R upayment:upayment /ms

WORKDIR /opt/backend
COPY --chown=upayment:upayment --from=build /ms/ms.jar ./ms.jar
#COPY  --from=build /ms/ms.jar ./ms.jar
# Set upayment it as the current user (instead of root)
USER upayment
EXPOSE 3007
ENTRYPOINT ["java","-jar","./ms.jar"]