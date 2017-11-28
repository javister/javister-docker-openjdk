# Базовый образ OpenJDK 8

[ ![Download](https://api.bintray.com/packages/javister/docker/javister%3Ajavister-docker-openjdk/images/download.svg) ](https://bintray.com/javister/docker/javister%3Ajavister-docker-openjdk/_latestVersion)
[![Build Status](https://travis-ci.org/javister/javister-docker-openjdk.svg?branch=master)](https://travis-ci.org/javister/javister-docker-openjdk)

Данный образ базируется на образе [javister-docker-base](https://github.com/javister/javister-docker-base).

Содержимое, добавляемое данным образом:

1. OpenJDK 8u, поставляемое в составе CentOS.
2. Шрифты MS Core Fonts, для возможности серверного формирования отчётов и подобных задач.
3. Преднастроенная переменная окружения JVM_OPTS. В данную переменную должны добавляться опции, требуемые контейнером. Приложения должны использовать эту переменную для активации накомленных параметров. Содержит:
    1. Настройки прокти, если прокси передан в переменной окружения http_proxy
    2. Исключения для прокси задаются переменной окружения JAVA_NON_PROXY, т.к. формат не совпадает с форматом переменной окружения no_proxy