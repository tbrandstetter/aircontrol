FROM amazoncorretto:21
ARG JAR_FILE=target/aircontrol*.jar

RUN yum update -y --security \
    && yum install -y python-setuptools  \
    && easy_install supervisor  \
    && yum install -y socat \
    && yum clean all  \
    && rm -rf /var/cache/yum

COPY supervisord.conf /etc/supervisor/supervisord.conf
COPY ${JAR_FILE} app.jar

CMD ["/usr/bin/supervisord"]