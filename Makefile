GRAALVM_HOME = $(HOME)/graalvm-ce-java11-21.3.0
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

uberjar:
	clj -A:uberjar

uberjar-run:
	java -cp target/bbssh-0.1.0-SNAPSHOT-standalone.jar clojure.main -m bbssh.core

uberjar-ls:
	jar tf target/bbssh-0.1.0-SNAPSHOT-standalone.jar

#
# Native image related targets
#
bbssh: resources/libbbssh.so $(CLOJURE_FILES)
	GRAALVM_HOME=$(GRAALVM_HOME) clj -A:native-image

native-image: bbssh

#
# Babashka related targets
#
test:
	bb test/bb.clj
