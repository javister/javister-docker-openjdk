#!/usr/bin/env bash

[ "$LOG_LEVEL" == "DEBUG" ] && echo -n " -showversion " >> /etc/container_environment/JVM_OPTS

# Если переменная установлена в пустое значение - то значит настраивать прокси не надо и выходим
[ -z "$PROXY" ] && exit 0

echo -n "-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT " >> /etc/container_environment/JVM_OPTS

[ "$JAVA_NON_PROXY" ] && echo -n "-Dhttp.nonProxyHosts=$JAVA_NON_PROXY " >> /etc/container_environment/JVM_OPTS

[ "$PROXY_USER" ] && echo -n "-Djdk.http.auth.tunneling.disabledSchemes= -Dhttp.proxyUser=$PROXY_USER -Dhttp.proxyPassword=$PROXY_PASS -Dhttps.proxyUser=$PROXY_USER -Dhttps.proxyPassword=$PROXY_PASS " >> /etc/container_environment/JVM_OPTS
