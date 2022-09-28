GRAALVM_HOME = $(HOME)/graalvm-ce-java11-22.2.0
ifeq (,$(findstring java11,$(GRAALVM_HOME)))
$(error Please use a Java 11 version of Graal)
endif
STATIC=false
PATH := $(GRAALVM_HOME)/bin:$(PATH)
VERSION = $(shell cat resources/BBSSH_VERSION)
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
JAVAC=$(JAVA_HOME)/bin/javac
INCLUDE_DIRS=$(shell find $(JAVA_HOME)/include -type d)
INCLUDE_ARGS=$(INCLUDE_DIRS:%=-I%) -I$(JNI_DIR)
CLOJURE_FILES=$(shell find src/clj -name *.clj)
ifeq ($(UNAME),Linux)
	LIB_FILE=resources/libbbssh.so
else ifeq ($(UNAME),FreeBSD)
	LIB_FILE=resources/libbbssh.so
else ifeq ($(UNAME),Darwin)
	LIB_FILE=resources/libbbssh.dylib
endif

.PHONY: clean run uberjar uberjar-run uberjar-ls native-image test

all: bbssh

clean:
	-rm -rf resources/libbbssh.so resources/libbbssh.dylib target bbssh

#
# C library related targets
#
$(CLASS_FILE): $(JAVA_FILE)
	$(JAVAC) $(JAVA_FILE)

$(C_HEADER): $(CLASS_FILE)
	mkdir -p $(JNI_DIR)
	$(JAVAC) -h $(JNI_DIR) $(JAVA_FILE)
	@touch $(C_HEADER)

$(SOLIB_FILE): $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -shared $(C_FILE) -o $(SOLIB_FILE) -fPIC

$(DYLIB_FILE):  $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -dynamiclib -undefined suppress -flat_namespace $(C_FILE) -o $(DYLIB_FILE) -fPIC

resources/libbbssh.so: $(SOLIB_FILE)
	mkdir -p resources
	cp $(SOLIB_FILE) resources

resources/libbbssh.dylib: $(DYLIB_FILE)
	mkdir -p resources
	cp $(DYLIB_FILE) resources

#
# Clojure related targets
#
run:
	clj -J-Djava.library.path=resources -m bbssh.core

#
# Native image related targets
#
build/bbssh: $(LIB_FILE) $(CLOJURE_FILES)
ifeq ($(STATIC),true)
	GRAALVM_HOME=$(GRAALVM_HOME) clojure -M:native-image-static
else
	GRAALVM_HOME=$(GRAALVM_HOME) clojure -M:native-image
endif
	mkdir -p build
	cp bbssh build

native-image: build/bbssh

package-linux: build/bbssh
	cd build && tar cvfz bbssh-$(VERSION)-linux-amd64.tgz bbssh

package-macos: build/bbssh
	cd build && zip bbssh-$(VERSION)-macos.zip bbssh

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
