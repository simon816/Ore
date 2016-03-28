FROM java:latest
MAINTAINER Walker Crouse <walkercrouse@hotmail.com>

RUN curl -O https://downloads.typesafe.com/typesafe-activator/1.3.9/typesafe-activator-1.3.9.zip
RUN unzip typesafe-activator-1.3.9.zip -d / && rm typesafe-activator-1.3.9.zip && chmod a+x /activator-dist-1.3.9/bin/activator
ENV PATH $PATH:/activator-dist-1.3.9

EXPOSE 9000 8888
RUN mkdir /app
WORKDIR /app

CMD ["activator", "run"]
