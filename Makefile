GRAALVM_HOME = $(HOME)/graalvm-ce-java11-22.2.0
ifeq (,$(findstring java11,$(GRAALVM_HOME)))
$(error Please use a Java 11 version of Graal)
endif
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
	GRAALVM_HOME=$(GRAALVM_HOME) clj -M:native-image

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
