# makepkg is available in bullseye's backports (and newer distros) only
# checkinstall is available in bullseye only, too
FROM debian:bullseye-backports

SHELL ["bash", "-x", "-c"]

ENV DEBIAN_FRONTEND=noninteractive

# note: don't build with OpenJDK > 16, as this is the runtime we ship
RUN apt-get update && \
    apt-get install -y openjdk-11-jdk git wget shellcheck bash make maven python3-minimal python3-pip \
        nsis zip libfuse2 checkinstall makepkg && \
    python3 -m pip install git+https://github.com/TheAssassin/appimagecraft.git#egg=appimagecraft

# note: makepkg is contained in universe only, so we need to add that repository

# create fake home directory so that maven can create a ~/.m2 directory
RUN mkdir /fakehome && \
    chmod 777 /fakehome
ENV HOME=/fakehome

# we shouldn't expect FUSE to work within Docker
ENV APPIMAGE_EXTRACT_AND_RUN=1
