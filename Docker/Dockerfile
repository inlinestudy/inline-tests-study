# docker build -t ssb4it .

# Use Ubuntu 24.04 LTS as the base image.
FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive
WORKDIR /root

RUN apt-get update
RUN apt-get install -y openjdk-8-jdk wget ssh git vim python3-pip maven
