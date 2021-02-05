ARG JAVISTER_BASE_VERSION
FROM javister-ntp.artifactory-jcr.krista.ru/javister-docker-base:${JAVISTER_BASE_VERSION}
MAINTAINER Viktor Verbitsky <vektory79@gmail.com>

ARG JDK_VERSION=1.8.0
ENV BASE_RPMLIST="${BASE_RPMLIST} java-${JDK_VERSION}-openjdk-devel cabextract xorg-x11-font-utils fontconfig"

RUN . /usr/local/bin/yum-proxy && \
    yum-install && \
    echo '*** Setup MS Core Fonts' && \
    https_proxy=$https_proxy rpm -i https://downloads.sourceforge.net/project/mscorefonts2/rpms/msttcore-fonts-installer-2.6-1.noarch.rpm && \
    echo '*** Clean up yum caches' && \
    yum-clean

ENV JVM_OPTS="" \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true" \
    JAVA_HOME="/usr/lib/jvm/java" \
    JAVA_NON_PROXY="" \
    JAVA_XMS="" \
    JAVA_XMX="" \
    DEBUG="false" \
    DEBUG_SUSPEND="false"

COPY src/main/docker /

RUN chmod --recursive +x /etc/my_init.d/*.sh /etc/service
