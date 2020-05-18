#!/usr/bin/env bash

[[ "${LOG_LEVEL}" == "DEBUG" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -showversion"

[[ "${JAVA_XMS}" ]] && export JVM_OPTS="${JVM_OPTS} -Xms${JAVA_XMS}"
[[ "${JAVA_XMX}" ]] && export JVM_OPTS="${JVM_OPTS} -Xmx${JAVA_XMX}"

if [[ "${DEBUG}" == "true" ]]; then
    export JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,address=*:8787"
    if [[ "${DEBUG_SUSPEND}" == "true" ]]; then
        export JVM_OPTS="${JVM_OPTS},suspend=y"
    else
        export JVM_OPTS="${JVM_OPTS},suspend=n"
    fi
fi

[[ "${LOCALE}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dfile.encoding=${LOCALE}"
[[ "${LANGUAGE}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Duser.language=${LANGUAGE}"
[[ "${COUNTRY}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Duser.country=${COUNTRY}"
[[ "${TZ}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Duser.timezone=${TZ}"

# Если переменная установлена в пустое значение - то значит настраивать прокси не надо и выходим
if [[ "${PROXY_HOST}" ]]; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY_HOST} -Dhttps.proxyPort=${PROXY_PORT}"

    [[ "${JAVA_NON_PROXY}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dhttp.nonProxyHosts=${JAVA_NON_PROXY}"

    [[ "${PROXY_USER}" ]] && export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Djdk.http.auth.tunneling.disabledSchemes= -Dhttp.proxyUser=${PROXY_USER} -Dhttp.proxyPassword=${PROXY_PASS} -Dhttps.proxyUser=${PROXY_USER} -Dhttps.proxyPassword=$PROXY_PASS"
fi

echo -n "${JAVA_TOOL_OPTIONS}" > /etc/container_environment/JAVA_TOOL_OPTIONS
echo -n "${JVM_OPTS}" > /etc/container_environment/JVM_OPTS

exit 0
