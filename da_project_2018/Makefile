JAVAC=javac
sources = $(shell find src -name '*.java')
classes = $(sources:src/%.java=bin/%.class)

all: bin binaries jar

bin:
	mkdir bin

binaries: $(classes)

bin/%.class: src/%.java
	$(JAVAC) -d bin -s src $<

jar:	
	@echo "Manifest-Version: 1.0" > manifest.mf
	@echo "Class-Path: ." >> manifest.mf
	@echo "Main-Class: main.Main" >> manifest.mf
	@echo "" >> manifest.mf
	
	jar cmf manifest.mf JARNAME.jar -C bin/ .

clean:
	rm -rf bin
	rm manifest.mf
