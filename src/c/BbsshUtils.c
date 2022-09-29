#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/un.h>
#include <sys/socket.h>
#include <termios.h>
#include <sys/ioctl.h>
#include "BbsshUtils.h"

/* move terminal into and out of raw mode for password entry */

static struct termios _saved_tio;
static int _in_raw_mode = 0;

void
leave_raw_mode(int quiet)
{
        if (!_in_raw_mode)
                return;
        if (tcsetattr(fileno(stdin), TCSADRAIN, &_saved_tio) == -1) {
                if (!quiet)
                        perror("tcsetattr");
        } else
                _in_raw_mode = 0;
}

void
enter_raw_mode(int quiet)
{
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
}
