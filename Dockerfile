FROM amazoncorretto:18.0.1
ARG JAR_FILE=aircontrol-service/target/*.jar

RUN yum update -y --security  \
 && yum install -y python-setuptools  \
 && easy_install supervisor  \
 && yum install -y socat \
 && yum clean all  \
 && rm -rf /var/cache/yum

COPY supervisord.conf /etc/supervisor/supervisord.conf
CMD ["/usr/bin/supervisord"]

COPY ${JAR_FILE} app.jar

CMD ["/usr/bin/supervisord"]