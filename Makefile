BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)/classes
HEADERS_DIR = $(BUILD_DIR)/headers
SRC_DIR = src/main
CPP_DIR = $(SRC_DIR)/cpp
LIB_DIR = $(BUILD_DIR)/lib
LIB_RES = $(SRC_DIR)/resources/lib/linux64

CLASSPATH = $(CLASSES_DIR)/kotlin/main
PACKAGE = com.github.lipen.jnisat

JMINISAT_CLASSNAME = $(PACKAGE).JMiniSat
JMINISAT_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JMINISAT_CLASSNAME)).h
JMINISAT_SRC = $(CPP_DIR)/JMiniSat.cpp
JMINISAT_LIBNAME = libjminisat.so
JMINISAT_LIB = $(LIB_DIR)/$(JMINISAT_LIBNAME)
JMINISAT_RES = $(LIB_RES)/$(JMINISAT_LIBNAME)
MINISAT_CPPFLAGS = -I/usr/local/include
MINISAT_LDFLAGS = -L/usr/local/lib -lminisat

JCADICAL_CLASSNAME = $(PACKAGE).JCadical
JCADICAL_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JCADICAL_CLASSNAME)).h
JCADICAL_SRC = $(CPP_DIR)/JCadical.cpp
JCADICAL_LIBNAME = libjcadical.so
JCADICAL_LIB = $(LIB_DIR)/$(JCADICAL_LIBNAME)
JCADICAL_RES = $(LIB_RES)/$(JCADICAL_LIBNAME)
CADICAL_CPPFLAGS = -I/usr/local/include
CADICAL_LDFLAGS = -L/usr/local/lib -lcadical

CLASSNAMES = $(JMINISAT_CLASSNAME) $(JCADICAL_CLASSNAME)
HEADERS = $(JMINISAT_HEADER) $(JCADICAL_HEADER)
LIBS = $(JMINISAT_LIB) $(JCADICAL_LIB)

JAVA_HOME ?= $(shell readlink -f /usr/bin/javac | sed "s:/bin/javac::")
JAVA_INCLUDE = $(JAVA_HOME)/include

CC = g++
CCFLAGS = -Wall -O3 -fPIC -fpermissive
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS = -shared -s

.PHONY: default libjminisat libjcadical libs headers classes res clean

default:
	@echo "Specify a target! [all libs libjminisat libjcadical headers classes res clean]"
	@echo " - libs -- Build all libraries"
	@echo " - libjminisat -- Build jminisat library"
	@echo " - libjcadical -- Build jcadical library"
	@echo " - headers -- Generate JNI headers from classes via javah"
	@echo " - classes -- Compile Java/Kotlin classes (run 'gradlew classes')"
	@echo " - res -- Copy libraries to '$(LIB_RES)'"
	@echo " - clean -- Run 'gradlew clean'"

all: classes headers libs res
libs: libjminisat libjcadical

libjminisat: LIB = $(JMINISAT_LIB)
libjminisat: SRC = $(JMINISAT_SRC)
libjminisat: CPPFLAGS += $(MINISAT_CPPFLAGS)
libjminisat: LDFLAGS += $(MINISAT_LDFLAGS)
libjminisat $(JMINISAT_LIB): $(JMINISAT_HEADER)
	@echo "=== Building libjminisat library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjcadical: LIB = $(JCADICAL_LIB)
libjcadical: SRC = $(JCADICAL_SRC)
libjcadical: CPPFLAGS += $(CADICAL_CPPFLAGS)
libjcadical: LDFLAGS += $(CADICAL_LDFLAGS)
libjcadical $(JCADICAL_LIB): $(JCADICAL_HEADER)
	@echo "=== Building libjcadical library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjminisat libjminisat: $(LIB_DIR)

$(LIB_DIR):
	@echo "=== Creating libdir..."
	mkdir -p $@

headers $(HEADERS): $(CLASSES_DIR)
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(CLASSPATH) $(CLASSNAMES)

classes $(CLASSES_DIR):
	@echo "=== Compiling classes..."
	./gradlew -q classes

res: $(LIBS)
	@echo "=== Copying libraries to resources..."
	install -d $(shell dirname $(JMINISAT_RES))
	install -m 644 $(JMINISAT_LIB) $(JMINISAT_RES)
	install -m 644 $(JCADICAL_LIB) $(JCADICAL_RES)

clean:
	@echo "=== Cleaning..."
	./gradlew clean
