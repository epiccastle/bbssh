void leave_raw_mode(int);
void enter_raw_mode(int);
int is_stdout_a_tty();
int get_terminal_width();
int get_terminal_height ();
int ssh_open_auth_socket (char *cpath);
void ssh_close_auth_socket(int socket);
int ssh_auth_socket_read(int fd, char *buffer, int count);
int ssh_auth_socket_write(int fd, char *buffer, int count);
