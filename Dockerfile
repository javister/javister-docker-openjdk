FROM javister-docker-docker.bintray.io/javister/javister-docker-base:1.0
MAINTAINER Viktor Verbitsky <vektory79@gmail.com>
LABEL java=8

COPY files /

ENV BASE_RPMLIST="$BASE_RPMLIST java-1.8.0-openjdk-devel cabextract xorg-x11-font-utils fontconfig"

RUN . /usr/local/sbin/yum-proxy && \
    yum-install && \
    echo '*** Setup MS Core Fonts' && \
    https_proxy=$https_proxy rpm -i https://downloads.sourceforge.net/project/mscorefonts2/rpms/msttcore-fonts-installer-2.6-1.noarch.rpm && \
    echo '*** Clean up yum caches' && \
    yum-clean && \
    chmod +x /etc/my_init.d/*.sh
