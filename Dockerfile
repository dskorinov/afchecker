FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM openjdk:17-alpine

LABEL title="Antispam bot AFChecker." 

RUN adduser -D telegram \
    && mkdir -p /afchecker \
    && chown -R telegram /afchecker

USER telegram

COPY --from=build /app/target/AFChecker-0.0.1-SNAPSHOT.jar /afchecker/AFChecker.jar

RUN mkdir -p /afchecker/database

WORKDIR /afchecker

CMD ["java", "-jar", "AFChecker.jar"]
