GRAALVM_HOME = $(HOME)/graalvm-ce-java11-22.2.0
ifeq (,$(findstring java11,$(GRAALVM_HOME)))
$(error Please use a Java 11 version of Graal)
endif
STATIC=true
MUSL=false
PATH := $(GRAALVM_HOME)/bin:$(PATH)
VERSION = $(shell cat .meta/VERSION)
UNAME = $(shell uname)
JNI_DIR=target/jni
CLASS_FILE=src/c/BbsshUtils.class
JAR_FILE=target/uberjar/bbssh-$(VERSION)-standalone.jar
SOLIB_FILE=$(JNI_DIR)/libbbssh.so
DYLIB_FILE=$(JNI_DIR)/libbbssh.dylib
JAVA_FILE=src/c/BbsshUtils.java
C_FILE=src/c/BbsshUtils.c
C_HEADER=$(JNI_DIR)/BbsshUtils.h
JAVA_HOME=$(GRAALVM_HOME)
INCLUDE_DIRS=$(shell find $(JAVA_HOME)/include -type d)
INCLUDE_ARGS=$(INCLUDE_DIRS:%=-I%) -I$(JNI_DIR)
CLOJURE_FILES=$(shell find src/clj -name *.clj)
ifeq ($(UNAME),Linux)
	LIB_FILE=$(SOLIB_FILE)
else ifeq ($(UNAME),FreeBSD)
	LIB_FILE=$(SOLIB_FILE)
else ifeq ($(UNAME),Darwin)
	LIB_FILE=$(DYLIB_FILE)
endif

.PHONY: clean run uberjar uberjar-run uberjar-ls native-image test

all: bbssh

clean:
	-rm -rf resources/libbbssh.so resources/libbbssh.dylib target bbssh

#
# C library related targets
#
$(CLASS_FILE): $(JAVA_FILE)
	javac $(JAVA_FILE)

$(C_HEADER): $(CLASS_FILE)
	mkdir -p $(JNI_DIR)
	javac -h $(JNI_DIR) $(JAVA_FILE)
	@touch $(C_HEADER)

$(SOLIB_FILE): $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -shared $(C_FILE) -o $(SOLIB_FILE) -fPIC

$(DYLIB_FILE):  $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -dynamiclib -undefined suppress -flat_namespace $(C_FILE) -o $(DYLIB_FILE) -fPIC

resources/libbbssh.so: $(LIB_FILE)
	mkdir -p resources
	cp $(SOLIB_FILE) resources

#
# Clojure related targets
#
run:
	clj -J-Djava.library.path=resources -m bbssh.core

#
# Native image related targets
#
bbssh: resources/libbbssh.so $(CLOJURE_FILES)
ifeq ($(MUSL),true)
	PATH=toolchain/x86_64-linux-musl-native/bin:$(PATH) GRAALVM_HOME=$(GRAALVM_HOME) clj -M:native-image-musl
else ifeq ($(STATIC),true)
	GRAALVM_HOME=$(GRAALVM_HOME) clj -M:native-image-static
else
	GRAALVM_HOME=$(GRAALVM_HOME) clj -M:native-image
endif

native-image: bbssh

#
# Babashka related targets
#
test:
	umask 0000; bb --config test/bb.edn -m bb-test.core

test-bb:
	BABASHKA_CLASSPATH=test umask 0000 && java -jar $(BABASHKA_SRC)/target/babashka-0.9.162-SNAPSHOT-standalone.jar test/bb_test/core.clj

codox:
	-rm -rf codox-processed
	mkdir codox-processed
	cp -a src/bb codox-processed
	sed -e "s/babashka.pods/'babashka.pods/" codox-processed/bb/pod/epiccastle/bbssh/impl/utils.clj -i
	sed -e "s/babashka.pods/'babashka.pods/" codox-processed/bb/pod/epiccastle/bbssh/agent.clj -i
	clj -X:codox

#
# musl toolchain
#
MUSL_PREBUILT_TOOLCHAIN_VERSION=10.2.1
ZLIB_VERSION=1.2.12
ARCH=x86_64
CURRENT_DIR=$(shell pwd)

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

toolchain-clean:
	rm -rf build toolchain
