GRAALVM_HOME = $(HOME)/graalvm-ce-java11-22.2.0
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
	LIB_FILE=libbbssh.a
else ifeq ($(UNAME),FreeBSD)
	LIB_FILE=libbbssh.a
else ifeq ($(UNAME),Darwin)
	LIB_FILE=libbbssh.a
else # windows
	LIB_FILE=bbssh.lib
endif

.PHONY: clean run uberjar uberjar-run uberjar-ls native-image test

all: bbssh

clean:
	-rm -rf resources/libbbssh.so resources/libbbssh.dylib target bbssh build

#
# C library related targets
#
$(CLASS_FILE): $(JAVA_FILE)
	$(JAVAC) $(JAVA_FILE)

libbbssh.a:
	$(JAVAC) -h src/c/native/ src/c/native/BbsshUtils.java
	$(CC) $(INCLUDE_ARGS) -c src/c/bbssh.c -o libbbssh.a

libbbssh.so:
	$(JAVAC) -h src/c/jni/ src/c/jni/BbsshUtils.java
	$(CC) $(INCLUDE_ARGS) -shared \
		-Isrc/c \
		src/c/jni/BbsshUtils.c \
		src/c/bbssh.c \
		-fPIC -o libbbssh.so

bbssh.lib:
	cl.exe -LD $(C_FILE)

#
# Clojure related targets
#
run:
	clojure -M:run-local

#
# Native image related targets
#

native-image:
	clojure -M:native-image

native-image-static-dynamic-lib-c:
	clojure -M:native-image-static-dynamic-lib-c

native-image-static:
	clojure -M:native-image-static

native-image-musl: toolchain
	PATH=toolchain/x86_64-linux-musl-native/bin:$(PATH) \
		clojure -M:native-image-musl


package-linux: build/bbssh
	cd build && tar cvfz bbssh-$(VERSION)-linux-amd64.tgz bbssh

package-macos: build/bbssh
	cd build && tar cvfz bbssh-$(VERSION)-macos-amd64.tgz bbssh

package-m1: build/bbssh
	cd build && tar cvfz bbssh-$(VERSION)-macos-aarch64.tgz bbssh

#
# Babashka related targets
#
test: $(LIB_FILE)
	-mkdir test/files/dir1/dir3
	umask 0000; bb --config test/bb.edn -m bb-test.core

test-bb: $(LIB_FILE)
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
ZLIB_VERSION=1.2.12
CURRENT_DIR = $(shell pwd)

toolchain/$(ARCH)-linux-musl-native/bin/gcc:
	-mkdir toolchain
	curl -L -o toolchain/$(ARCH)-linux-musl-native.tgz https://more.musl.cc/$(MUSL_PREBUILT_TOOLCHAIN_VERSION)/$(ARCH)-linux-musl/$(ARCH)-linux-musl-native.tgz
	cd toolchain && tar xvfz $(ARCH)-linux-musl-native.tgz

musl: toolchain/$(ARCH)-linux-musl-native/bin/gcc

build/zlib/zlib-$(ZLIB_VERSION).tar.gz:
	-mkdir -p build/zlib
	curl -L -o build/zlib/zlib-$(ZLIB_VERSION).tar.gz https://zlib.net/zlib-$(ZLIB_VERSION).tar.gz

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
