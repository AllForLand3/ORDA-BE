FROM tomcat:8.5.95-jdk17-corretto-al2

RUN yum install -y shadow-utils \
    && yum clean all \
    && groupadd --system --gid 1001 tomcatuser \
    && useradd --system --uid 1001 --gid 1001 --home-dir /usr/local/tomcat \
        --shell /sbin/nologin --no-create-home tomcatuser \
    && mkdir -p /usr/local/tomcat/webapps /usr/local/tomcat/logs \
        /usr/local/tomcat/temp /usr/local/tomcat/work \
    && chown -R tomcatuser:tomcatuser /usr/local/tomcat/webapps \
        /usr/local/tomcat/logs /usr/local/tomcat/temp /usr/local/tomcat/work \
    && chmod -R u+rwX,go-w /usr/local/tomcat/webapps /usr/local/tomcat/logs \
        /usr/local/tomcat/temp /usr/local/tomcat/work

COPY target/orda-api.war /usr/local/tomcat/webapps/orda-api.war
RUN chmod 644 /usr/local/tomcat/webapps/orda-api.war

USER tomcatuser:tomcatuser

EXPOSE 8080
