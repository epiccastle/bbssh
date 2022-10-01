import static java.util.Objects.requireNonNull;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.lang.Thread.currentThread;
import static java.util.Locale.ENGLISH;

//import com.oracle.svm.core.SubstrateOptions;
//import com.oracle.svm.core.option.OptionUtils;
//import com.oracle.svm.core.c.ProjectHeaderFile;
//import com.oracle.svm.core.c.CGlobalData;
//import com.oracle.svm.core.c.CGlobalDataFactory;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.Pointer;
import org.graalvm.word.PointerBase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CContext(BbsshUtils.Directives.class)
public final class BbsshUtils {

    public static final class Directives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList("\"bbssh.h\"");
        }

        @Override
        public List<String> getLibraries() {
            return Arrays.asList("bbssh");
        }
    }

    @CFunction("leave_raw_mode")
    public static native void leave_raw_mode(int quiet);

    @CFunction("enter_raw_mode")
    public static native void enter_raw_mode(int quiet);

    @CFunction("is_stdout_a_tty")
    public static native int is_stdout_a_tty();

    @CFunction("get_terminal_width")
    public static native int get_terminal_width();

    @CFunction("get_terminal_height")
    public static native int get_terminal_height();

    @CFunction("ssh_open_auth_socket")
    public static native int
        cssh_open_auth_socket(CCharPointer cpath);

    public static int ssh_open_auth_socket(String path)
    {
        return cssh_open_auth_socket(
            org.graalvm.nativeimage.c.type.CTypeConversion.toCString(path).get()
        );
    }

    @CFunction("ssh_close_auth_socket")
    public static native int ssh_close_auth_socket(int sock_fd);

    @CFunction("ssh_auth_socket_read")
    public static native int
        cssh_auth_socket_read(int fd,
                              CCharPointer buffer,
                              int count);

    public static int ssh_auth_socket_read(int fd, byte[] buf, int count)
    {
        return cssh_auth_socket_read
            (
             fd,
             org.graalvm.nativeimage.c.type.CTypeConversion.toCBytes(buf).get(),
             count
             );

    }

    @CFunction("ssh_auth_socket_write")
    public static native int
        cssh_auth_socket_write(int fd,
                              CCharPointer buffer,
                              int count);

    public static int ssh_auth_socket_write(int fd, byte[] buf, int count)
    {
        return cssh_auth_socket_write
            (
             fd,
             org.graalvm.nativeimage.c.type.CTypeConversion.toCBytes(buf).get(),
             count
             );
    }
}
