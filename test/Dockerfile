FROM alpine:3.16.2
ARG root_password
RUN apk update
RUN apk add openssh-server openssh file curl
RUN sed -E "s/^#*(PermitRootLogin ).+$/\1yes/" /etc/ssh/sshd_config -i
RUN sed -E "s/^#*(AllowTcpForwarding ).+$/\1yes/" /etc/ssh/sshd_config -i
RUN ssh-keygen -A
RUN echo "root:$root_password"|chpasswd
ENTRYPOINT ["/usr/sbin/sshd","-D","-E","/tmp/sshd.log"]
EXPOSE 22/tcp