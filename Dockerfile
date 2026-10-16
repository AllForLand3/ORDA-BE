FROM tomcat:8.5.95-jdk17-corretto-al2

COPY target/orda-api.war /usr/local/tomcat/webapps/orda-api.war

EXPOSE 8080
