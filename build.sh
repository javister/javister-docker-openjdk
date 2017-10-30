#!/bin/bash -e

VERSION=1.0.java8
DATE=$(date +"%Y-%m-%d")

IMAGE_TAG="javister-docker-docker.bintray.io/javister/javister-docker-openjdk"
PROXY_ARGS="--build-arg http_proxy=${http_proxy} \
            --build-arg no_proxy=${no_proxy}"
docker pull javister-docker-docker.bintray.io/javister/javister-docker-base:1.0

docker build \
    --tag ${IMAGE_TAG}:latest \
    --tag ${IMAGE_TAG}:${VERSION} \
    --tag ${IMAGE_TAG}:${VERSION}-${DATE} \
    ${PROXY_ARGS} \
    $@ \
    .

docker push ${IMAGE_TAG}:latest
docker push ${IMAGE_TAG}:${VERSION}
docker push ${IMAGE_TAG}:${VERSION}-${DATE}
