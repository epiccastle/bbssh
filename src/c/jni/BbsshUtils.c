#include "bbssh.h"
#include "BbsshUtils.h"
#include <jni.h>

/*
 * Class:     BbsshUtils
 * Method:    is_stdout_a_tty
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_is_1stdout_1a_1tty
  (JNIEnv *env, jclass this)
{
  return (jint)is_stdout_a_tty();
}

/*
 * Class:     BbsshUtils
 * Method:    get_terminal_width
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_get_1terminal_1width
  (JNIEnv *env, jclass this)
{
  return (jint)get_terminal_width();
}

/*
 * Class:     BbsshUtils
 * Method:    get_terminal_height
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_get_1terminal_1height
  (JNIEnv *env, jclass this)
{
  return (jint)get_terminal_height();
}

/*
 * Class:     BbsshUtils
 * Method:    enter_raw_mode
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_BbsshUtils_enter_1raw_1mode
  (JNIEnv *env, jclass this, jint quiet)
{
  enter_raw_mode((int)quiet);
}

/*
 * Class:     BbsshUtils
 * Method:    leave_raw_mode
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_BbsshUtils_leave_1raw_1mode
  (JNIEnv *env, jclass this, jint quiet)
{
  leave_raw_mode((int)quiet);
}

/*
 * Class:     BbsshUtils
 * Method:    ssh_open_auth_socket
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_ssh_1open_1auth_1socket
  (JNIEnv *env, jclass this, jstring path)
{
  const char* cpath = (*env)->GetStringUTFChars(env, path, 0);
  jint result = (jint)ssh_open_auth_socket(cpath);
  (*env)->ReleaseStringUTFChars(env, path, cpath);
  return result;
}

/*
 * Class:     BbsshUtils
 * Method:    ssh_close_auth_socket
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_BbsshUtils_ssh_1close_1auth_1socket
  (JNIEnv *env, jclass this, jint sock_fd)
{
  ssh_close_auth_socket((int)sock_fd);
}

/*
 * Class:     BbsshUtils
 * Method:    ssh_auth_socket_read
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_ssh_1auth_1socket_1read
  (JNIEnv *env, jclass this, jint sock_fd, jbyteArray buf, jint size)
{
  jbyte buffer[size];
  int bytes_read = ssh_auth_socket_read(sock_fd, (void *)buffer, size);
  (*env)->SetByteArrayRegion(env, buf, 0, bytes_read, buffer);
  return (jint)bytes_read;
}

/*
 * Class:     BbsshUtils
 * Method:    ssh_auth_socket_write
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_BbsshUtils_ssh_1auth_1socket_1write
  (JNIEnv *env, jclass this, jint sock_fd, jbyteArray buf, jint size)
{
  jboolean copy = 1;
  jbyte *buffer = (*env)->GetByteArrayElements(env, buf, &copy);
  int bytes_writen = ssh_auth_socket_write(sock_fd, (const void *)buffer, size);
  (*env)->ReleaseByteArrayElements(env, buf, buffer, JNI_ABORT);
  return (jint)bytes_writen;
}
