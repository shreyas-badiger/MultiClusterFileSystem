FROM ubuntu
USER root

RUN apt-get update
RUN apt-get install -y openssh-server

#Setup SSH 
RUN mkdir /var/run/sshd
RUN echo 'root:root' |chpasswd
RUN sed -ri 's/^#?PermitRootLogin\s+.*/PermitRootLogin yes/' /etc/ssh/sshd_config
RUN sed -ri 's/UsePAM yes/#UsePAM yes/g' /etc/ssh/sshd_config
RUN mkdir /root/.ssh
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
EXPOSE 22
CMD    ["/usr/sbin/sshd", "-D"]

#Install net tools
RUN apt-get update -y
RUN apt-get install net-tools -y
RUN apt-get install iputils-ping -y
RUN apt-get install iproute2 -y

#Install dev tools
RUN apt-get install vim -y

#Install Java
RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install software-properties-common -y
RUN apt install default-jdk -y
RUN java -version
RUN apt install openjdk-8-jdk -y

#Setup Hadoop
COPY hadoop.tar.gz /root
RUN cd /root && pwd && ls -lart && tar -zxvf hadoop.tar.gz
