FROM openjdk:8-jdk

# Dependencies
RUN apt-get update && \
    apt-get install -y \
      libsnappy1v5 \
      python-pip \
      python-virtualenv \
      python-dev \
      rsync \