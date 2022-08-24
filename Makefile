GRAALVM = $(HOME)/graalvm-ce-java11-21.3.0
ifeq (,$(findstring java11,$(GRAALVM)))
$(error Please use a Java 11 version of Graal)
endif
PATH := $(GRAALVM)/bin:$(PATH)
VERSION = $(shell cat .meta/VERSION)
UNAME = $(shell uname)
JNI_DIR=target/jni
CLASS_DIR=target/default/classes
CLASS_NAME=BbsshUtils
CLASS_FILE=$(CLASS_DIR)/$(CLASS_NAME).class
JAR_FILE=target/uberjar/bbssh-$(VERSION)-standalone.jar
SOLIB_FILE=$(JNI_DIR)/libbbssh.so
DYLIB_FILE=$(JNI_DIR)/libbbssh.dylib
JAVA_FILE=src/c/BbsshUtils.java
C_FILE=src/c/BbsshUtils.c
C_HEADER=$(JNI_DIR)/BbsshUtils.h
ifndef JAVA_HOME
	JAVA_HOME=$(GRAALVM)
endif
INCLUDE_DIRS=$(shell find $(JAVA_HOME)/include -type d)
INCLUDE_ARGS=$(INCLUDE_DIRS:%=-I%) -I$(JNI_DIR)
ifeq ($(UNAME),Linux)
	LIB_FILE=$(SOLIB_FILE)
else ifeq ($(UNAME),FreeBSD)
	LIB_FILE=$(SOLIB_FILE)
else ifeq ($(UNAME),Darwin)
	LIB_FILE=$(DYLIB_FILE)
endif


$(CLASS_FILE): $(JAVA_FILE)
	javac $(JAVA_FILE)

header: $(C_HEADER)

$(C_HEADER): $(CLASS_FILE)
	mkdir -p $(JNI_DIR)
	javac -h $(JNI_DIR) $(JAVA_FILE)
	@touch $(C_HEADER)

lib: $(LIB_FILE)

$(SOLIB_FILE): $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -shared $(C_FILE) -o $(SOLIB_FILE) -fPIC
	cp $(SOLIB_FILE) ./
	mkdir -p resources
	cp $(SOLIB_FILE) ./resources/

$(DYLIB_FILE):  $(C_FILE) $(C_HEADER)
	$(CC) $(INCLUDE_ARGS) -dynamiclib -undefined suppress -flat_namespace $(C_FILE) -o $(DYLIB_FILE) -fPIC
	cp $(DYLIB_FILE) ./
	mkdir -p resources
	cp $(DYLIB_FILE) ./resources/
	make copy-libs-to-resource
