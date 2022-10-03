#ifndef _WIN32
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/un.h>
#include <sys/socket.h>
#include <termios.h>
#include <sys/ioctl.h>
#else
#include <windows.h>
#endif

#include "bbssh.h"

/* move terminal into and out of raw mode for password entry */

#ifndef _WIN32

static struct termios _saved_tio;
static int _in_raw_mode = 0;
#endif

void
leave_raw_mode(int quiet)
{
#ifndef _WIN32
        if (!_in_raw_mode)
                return;
        if (tcsetattr(fileno(stdin), TCSADRAIN, &_saved_tio) == -1) {
                if (!quiet)
                        perror("tcsetattr");
        } else
                _in_raw_mode = 0;
#else
        HANDLE stdin_handle=GetStdHandle(STD_INPUT_HANDLE);
        DWORD mode;
        if(GetConsoleMode(stdin_handle, &mode))
          {
            SetConsoleMode(stdin_handle, mode | ENABLE_ECHO_INPUT);
          }
#endif
}

void
enter_raw_mode(int quiet)
{
#ifndef _WIN32
        struct termios tio;

        if (tcgetattr(fileno(stdin), &tio) == -1) {
                if (!quiet)
                        perror("tcgetattr");
                return;
        }
        _saved_tio = tio;
        tio.c_iflag |= IGNPAR;
        tio.c_iflag &= ~(ISTRIP | INLCR | IGNCR | ICRNL | IXON | IXANY | IXOFF);
#ifdef IUCLC
        tio.c_iflag &= ~IUCLC;
#endif
        tio.c_lflag &= ~(ISIG | ICANON | ECHO | ECHOE | ECHOK | ECHONL);
#ifdef IEXTEN
        tio.c_lflag &= ~IEXTEN;
#endif
        tio.c_oflag &= ~OPOST;
        tio.c_cc[VMIN] = 1;
        tio.c_cc[VTIME] = 0;
        if (tcsetattr(fileno(stdin), TCSADRAIN, &tio) == -1) {
                if (!quiet)
                        perror("tcsetattr");
        } else
                _in_raw_mode = 1;
#else
        HANDLE stdin_handle=GetStdHandle(STD_INPUT_HANDLE);
        DWORD mode;
        if(GetConsoleMode(stdin_handle, &mode))
          {
            SetConsoleMode(stdin_handle, mode & ~ENABLE_ECHO_INPUT);
          }
#endif
}

int is_stdout_a_tty() {
#ifndef _WIN32
  return isatty(STDOUT_FILENO);
#else
  return GetFileType(GetStdHandle(STD_OUTPUT_HANDLE))==FILE_TYPE_CHAR?1:0;
#endif
}

#ifndef _WIN32
static int get_win_size(int fd, struct winsize *win) {
  return ioctl(fd, TIOCGWINSZ, (char*)win);
}
#endif

int get_terminal_width () {
#ifndef _WIN32
  struct winsize size;
  (void)get_win_size(STDOUT_FILENO, &size);
  return (size.ws_col);
#else
  HANDLE stdout_handle=GetStdHandle(STD_OUTPUT_HANDLE);
  CONSOLE_SCREEN_BUFFER_INFO info;
  if(GetConsoleScreenBufferInfo(stdout_handle, &info)) {
    return info.dwSize.X;
  } else {
    // fake width if it fails
    return 80;
  }
#endif
}

int get_terminal_height () {
#ifndef _WIN32
  struct winsize size;
  (void)get_win_size(STDOUT_FILENO, &size);
  return (size.ws_row);
#else
  HANDLE stdout_handle=GetStdHandle(STD_OUTPUT_HANDLE);
  CONSOLE_SCREEN_BUFFER_INFO info;
  if(GetConsoleScreenBufferInfo(stdout_handle, &info)) {
    return info.dwSize.Y;
  } else {
    // fake width if it fails
    return 80;
  }
#endif
}

#ifndef _WIN32
/*
 * Copy string src to buffer dst of size dsize.  At most dsize-1
 * chars will be copied.  Always NUL terminates (unless dsize == 0).
 * Returns strlen(src); if retval >= dsize, truncation occurred.
 */
size_t
bbssh_strlcpy(char * __restrict dst, const char * __restrict src, size_t dsize)
{
        const char *osrc = src;
        size_t nleft = dsize;

        /* Copy as many bytes as will fit. */
        if (nleft != 0) {
                while (--nleft != 0) {
                        if ((*dst++ = *src++) == '\0')
                                break;
                }
        }

        /* Not enough room in dst, add NUL and traverse rest of src. */
        if (nleft == 0) {
                if (dsize != 0)
                        *dst = '\0';		/* NUL-terminate dst */
                while (*src++)
                        ;
        }

        return(src - osrc - 1);	/* count does not include NUL */
}
#endif

/* same error as openbsd ssh code uses */
#define SSH_ERR_SYSTEM_ERROR -24

int ssh_open_auth_socket (const char *cpath) {
#ifndef _WIN32
  struct sockaddr_un sunaddr;
  memset(&sunaddr, 0, sizeof(sunaddr));
  sunaddr.sun_family = AF_UNIX;
  bbssh_strlcpy(sunaddr.sun_path, cpath, sizeof(sunaddr.sun_path));

  int sock = socket(AF_UNIX, SOCK_STREAM, 0);
  if(sock == -1)
    {
      // error: failed to allocate unix domain socket
      return SSH_ERR_SYSTEM_ERROR;
    }

  if(fcntl(sock, F_SETFD, FD_CLOEXEC) == -1 ||
     connect(sock, (struct sockaddr *)&sunaddr, sizeof(sunaddr)) == -1)
    {
      close(sock);
      return SSH_ERR_SYSTEM_ERROR;
    }

  return sock;
#else
  HANDLE result = CreateFileW
    (
     cpath,
     GENERIC_READ | GENERIC_WRITE,
     0,
     NULL,
     OPEN_EXISTING,
     FILE_ATTRIBUTE_NORMAL,
     NULL);

  if(result==-1)
    {
      wchar_t buf[256];
      printf("error: %d\n", GetLastError());
      FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                     NULL, GetLastError(), MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                     buf, (sizeof(buf)), NULL);
      printf(buf);
      printf("\n");
      return -1;
    }

  return result;

#endif
}

void ssh_close_auth_socket(int socket)
{
#ifndef _WIN32
  close(socket);
#else
  CloseHandle(socket);
#endif
}

int ssh_auth_socket_read(int fd, void *buffer, int count)
{
#ifndef _WIN32
  return read(fd, buffer, count);
#else
  DWORD bytes_read;
  if(ReadFile(fd,buffer,count,&bytes_read, NULL))
    return bytes_read;
  else
    return -1;
#endif
}

int ssh_auth_socket_write(int fd, const void *buffer, int count)
{
#ifndef _WIN32
  return write(fd, buffer, count);
#else
  DWORD bytes_written;
  if(WriteFile(fd,buffer,count,&bytes_written, NULL))
    return bytes_written;
  else
    return -1;
#endif
}
