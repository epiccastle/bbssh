GRAALVM_HOME = $(HOME)/graalvm-ce-java11-22.3.0
ifeq (,$(findstring java11,$(GRAALVM_HOME)))
$(error Please use a Java 11 version of Graal)
endif
STATIC=false
PATH := $(GRAALVM_HOME)/bin:$(PATH)
VERSION = $(shell cat resources/BBSSH_VERSION)
UNAME = $(shell uname)
CLASS_FILE=src/c/BbsshUtils.class
JAVA_FILE=src/c/BbsshUtils.java
C_FILE=src/c/BbsshUtils.c
C_HEADER=src/c/BbsshUtils.h
JAVA_HOME=$(GRAALVM_HOME)
JAVAC=$(JAVA_HOME)/bin/javac
INCLUDE_DIRS=$(shell find $(JAVA_HOME)/include -type d)
INCLUDE_ARGS=$(INCLUDE_DIRS:%=-I%)
CLOJURE_FILES=$(shell find src/clj -name '*.clj')
ifeq ($(UNAME),Linux)
	NATIVE_LIB_FILE=build/libbbssh.a
	JNI_LIB_FILE=build/libbbssh.so
	FLAVOUR=linux
else ifeq ($(UNAME),FreeBSD)
	NATIVE_LIB_FILE=build/libbbssh.a
	JNI_LIB_FILE=build/libbbssh.so
	FLAVOUR=unix
else ifeq ($(UNAME),Darwin)
	NATIVE_LIB_FILE=build/libbbssh.a
	JNI_LIB_FILE=build/libbbssh.dylib
	FLAVOUR=mac
else
	NATIVE_LIB_FILE=build/bbssh.lib
	JNI_LIB_FILE=build/bbssh.dll
	FLAVOUR=windows
endif

.PHONY: clean run uberjar uberjar-run uberjar-ls native-image test

all: build/bbssh

clean:
	-rm -rf resources/libbbssh.so resources/libbbssh.dylib target bbssh build src/c/native/BbsshUtils.class src/c/jni/BbsshUtils.class

#
# C library related targets
#
$(CLASS_FILE): $(JAVA_FILE)
	$(JAVAC) $(JAVA_FILE)

src/c/native/BbsshUtils.class: src/c/native/BbsshUtils.java
	$(JAVAC) -h src/c/native/ src/c/native/BbsshUtils.java

src/c/jni/BbsshUtils.class: src/c/jni/BbsshUtils.java
	$(JAVAC) -h src/c/jni/ src/c/jni/BbsshUtils.java

build/libbbssh.a: src/c/native/BbsshUtils.class
	-mkdir build
	$(CC) $(INCLUDE_ARGS) -c src/c/bbssh.c -o build/libbbssh.a

build/libbbssh.so: src/c/jni/BbsshUtils.class
	-mkdir build
	$(CC) $(INCLUDE_ARGS) -shared \
		-Isrc/c \
		src/c/jni/BbsshUtils.c \
		src/c/bbssh.c \
		-fPIC -o build/libbbssh.so

build/libbbssh.dylib: src/c/jni/BbsshUtils.class
	-mkdir build
	$(CC) $(INCLUDE_ARGS) -dynamiclib \
		-undefined suppress \
		-flat_namespace \
		-Isrc/c \
		src/c/jni/BbsshUtils.c \
		src/c/bbssh.c \
		-fPIC -o build/libbbssh.dylib

build/bbssh.lib:
	cl.exe -LD $(C_FILE)

#
# Clojure related targets
#
run: $(JNI_LIB_FILE)
	clojure -M:run-local

#
# Native image related targets
#

native-image: $(NATIVE_LIB_FILE)
	clojure -M:native-image

native-image-static-dynamic-lib-c: $(NATIVE_LIB_FILE)
	clojure -M:native-image-static-dynamic-lib-c

native-image-static: $(NATIVE_LIB_FILE)
	clojure -M:native-image-static

native-image-musl: toolchain $(NATIVE_LIB_FILE)
	PATH=toolchain/x86_64-linux-musl-native/bin:$(PATH) \
		clojure -M:native-image-musl

ifeq ($(FLAVOUR),linux)
build/bbssh: native-image-musl
else
build/bbssh: native-image
endif

package-linux: native-image-musl
	cd build && tar cvfz bbssh-$(VERSION)-linux-amd64.tgz bbssh
	-mkdir dist
	cp build/bbssh build/bbssh-$(VERSION)-linux-amd64.tgz dist

package-macos: native-image
	cd build && tar cvfz bbssh-$(VERSION)-macos-amd64.tgz bbssh
	-mkdir dist
	cp build/bbssh build/bbssh-$(VERSION)-macos-amd64.tgz dist

package-m1: native-image
	cd build && tar cvfz bbssh-$(VERSION)-macos-aarch64.tgz bbssh
	-mkdir dist
	cp build/bbssh build/bbssh-$(VERSION)-macos-aarch64.tgz dist
#
# Babashka related targets
#
test: $(JNI_LIB_FILE)
	-mkdir test/files/dir1/dir3
	umask 0000; bb --config test/bb.edn -m bb-test.core

test-bb: $(JNI_LIB_FILE)
	-mkdir test/files/dir1/dir3
	BABASHKA_CLASSPATH=test umask 0000 && java -jar $(BABASHKA_SRC)/target/babashka-0.9.162-SNAPSHOT-standalone.jar test/bb_test/core.clj

codox:
	-rm -rf codox-processed
	mkdir codox-processed
	cp -a src/bb codox-processed
	sed -e "s/babashka.pods/'babashka.pods/" codox-processed/bb/pod/epiccastle/bbssh/impl/utils.clj -i
	sed -e "s/babashka.pods/'babashka.pods/" codox-processed/bb/pod/epiccastle/bbssh/agent.clj -i
	clj -X:codox

codox-upload:
	rsync -av --delete target/docs/ www-data@epiccastle.io:~/epiccastle.io/public/bbssh/


#
# musl toolchain
#
ARCH=x86_64
MUSL_PREBUILT_TOOLCHAIN_VERSION=10.2.1
ZLIB_VERSION=1.2.13
CURRENT_DIR = $(shell pwd)

toolchain/$(ARCH)-linux-musl-native/bin/gcc:
	-mkdir toolchain
	curl -k -L -o toolchain/$(ARCH)-linux-musl-native.tgz https://more.musl.cc/$(MUSL_PREBUILT_TOOLCHAIN_VERSION)/$(ARCH)-linux-musl/$(ARCH)-linux-musl-native.tgz
	cd toolchain && tar xvfz $(ARCH)-linux-musl-native.tgz

musl: toolchain/$(ARCH)-linux-musl-native/bin/gcc

build/zlib/zlib-$(ZLIB_VERSION).tar.gz:
	-mkdir -p build/zlib
	curl -k -L -o build/zlib/zlib-$(ZLIB_VERSION).tar.gz https://zlib.net/zlib-$(ZLIB_VERSION).tar.gz

build/zlib/zlib-$(ZLIB_VERSION)/src: build/zlib/zlib-$(ZLIB_VERSION).tar.gz
	-mkdir -p build/zlib/zlib-$(ZLIB_VERSION)
	tar -xvzf build/zlib/zlib-$(ZLIB_VERSION).tar.gz -C build/zlib/zlib-$(ZLIB_VERSION) --strip-components 1
	touch build/zlib/zlib-$(ZLIB_VERSION)/src

toolchain/$(ARCH)-linux-musl-native/lib/libz.a: build/zlib/zlib-$(ZLIB_VERSION)/src
	-mkdir toolchain
	cd build/zlib/zlib-$(ZLIB_VERSION) && \
	./configure --static --prefix=$(CURRENT_DIR)/toolchain/$(ARCH)-linux-musl-native && \
	make PATH=$(CURRENT_DIR)/toolchain/$(ARCH)-linux-musl-native/bin:$$PATH CC=$(ARCH)-linux-musl-gcc && \
	make install

libz: toolchain/$(ARCH)-linux-musl-native/lib/libz.a

toolchain: musl libz
