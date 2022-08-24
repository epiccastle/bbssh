public class BbsshUtils {
    // terminal tty
    public static native int is_a_tty();

    // terminal size queries
    public static native int get_terminal_width();
    public static native int get_terminal_height();

    // terminal raw mode
    public static native void enter_raw_mode(int quiet);
    public static native void leave_raw_mode(int quiet);

    // unix domain socket access
    public static native int ssh_open_auth_socket(String path);
    public static native void ssh_close_auth_socket(int socket);
    public static native int ssh_auth_socket_read(int fd, byte[] buf, int count);
    public static native int ssh_auth_socket_write(int fd, byte[] buf, int count);
}
