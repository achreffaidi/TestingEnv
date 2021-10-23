FROM maven:3.6.0-jdk-11-slim
COPY src /home/app/src
COPY pom.xml /home/app
COPY test.sh /home/app
RUN chmod 777 /home/app/test.sh
WORKDIR /home/app
CMD ["./test.sh"]
