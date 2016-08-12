FROM java:latest
MAINTAINER Luke Granger-Brown <lukegb@spongepowered.org>

COPY target/universal/ore.tgz /app.tgz
RUN tar -xf /app.tgz && sh -c 'ln -s /ore-* /app' && rm -f /app/conf/application.conf && mv /app/conf/application.conf.template /app/conf/application.conf && cat /app/conf/application.conf && ls -a /app

EXPOSE 9000

ARG BUILD_NUMBER
ARG GIT_BRANCH
ARG GIT_COMMIT
ARG JOB_NAME
ARG BUILD_TAG
ENV BUILD_NUMBER ${BUILD_NUMBER}
ENV GIT_BRANCH ${GIT_BRANCH}
ENV GIT_COMMIT ${GIT_COMMIT}
ENV JOB_NAME ${JOB_NAME}
ENV BUILD_TAG ${BUILD_TAG}

CMD ["/app/bin/ore"]
