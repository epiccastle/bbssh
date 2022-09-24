FROM alpine:latest
ARG root_password
RUN apk update
RUN apk add openssh-server openssh
RUN sed -E "s/^#(PermitRootLogin ).+$/\1yes/" /etc/ssh/sshd_config -i
RUN ssh-keygen -A
RUN echo "root:$root_password"|chpasswd
ENTRYPOINT ["/usr/sbin/sshd","-D"]
EXPOSE 22/tcp