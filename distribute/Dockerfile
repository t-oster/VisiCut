FROM debian:trixie

SHELL ["bash", "-x", "-c"]

ENV DEBIAN_FRONTEND=noninteractive

# workaround nsis bug: https://sourceforge.net/p/nsis/bugs/1180/
ENV LANG C.UTF-8

RUN apt-get update && \
    apt-get install -y default-jdk git wget shellcheck bash make maven python3-minimal python3-pip \
        nsis zip libfuse2 checkinstall makepkg fonts-noto-extra potrace sudo librsvg2-bin && \
    python3 -m pip install --break-system-packages git+https://github.com/TheAssassin/appimagecraft.git@6b36fda#egg=appimagecraft

# we don't have a defined $HOME, but we need Maven to write the libLaserCut artifact somewhere
COPY docker/maven-settings.xml /usr/share/maven/conf/settings.xml

# we shouldn't expect FUSE to work within Docker
ENV APPIMAGE_EXTRACT_AND_RUN=1

VOLUME /visicut
RUN git config --global --add safe.directory /visicut
