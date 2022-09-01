FROM alpine:latest
RUN apk update
RUN apk add openssh-server
RUN sed -E "s/^#(PermitRootLogin ).+$/\1yes/" /etc/ssh/sshd_config -i
RUN ssh-keygen -A
RUN echo "root:password"|chpasswd
ENTRYPOINT ["/usr/sbin/sshd","-D"]
EXPOSE 22/tcp