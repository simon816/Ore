FROM java:latest
MAINTAINER Luke Granger-Brown <lukegb@spongepowered.org>

COPY target/universal/ore.tgz /app.tgz
RUN tar -xf /app.tgz && sh -c 'ln -s /ore-* /app' && yes | cp -f /app/conf/application.conf.template /app/conf/application.conf && ls -a /app

EXPOSE 9000
CMD ["/app/bin/ore"]
