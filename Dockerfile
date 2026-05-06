ARG RUNTIME_IMAGE=eclipse-temurin:17.0.14_7-jre-jammy
FROM ${RUNTIME_IMAGE}
WORKDIR /app

# Ubuntu 22.04 (jammy) jammy-updates 中的 metapackage 版本，见 https://packages.ubuntu.com/jammy-updates/libreoffice
ARG LIBREOFFICE_VERSION=1:7.3.7-0ubuntu0.22.04.10
RUN apt-get update \
    && apt-get install -y --no-install-recommends libreoffice=${LIBREOFFICE_VERSION} \
    && rm -rf /var/lib/apt/lists/*

COPY target/doc_server.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
