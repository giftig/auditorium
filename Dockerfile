FROM java:8-jre

EXPOSE 80

COPY build/lib/ /usr/app/lib
COPY build/auditorium.jar /usr/app/auditorium.jar

WORKDIR /usr/app
RUN java -jar auditorium.jar
