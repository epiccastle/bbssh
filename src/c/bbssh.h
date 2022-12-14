#ifndef _BBSSH_H_
#define _BBSSH_H_

void leave_raw_mode(int);
void enter_raw_mode(int);
int is_stdout_a_tty();
int get_terminal_width();
int get_terminal_height ();
int ssh_open_auth_socket (const char *cpath);
void ssh_close_auth_socket(int socket);
int ssh_auth_socket_read(int fd, void *buffer, int count);
int ssh_auth_socket_write(int fd, const void *buffer, int count);

#endif
