all: 
	./gradlew testJar
	cp build/libs/*.jar .

fifo:
	./gradlew fifoJar
	cp build/libs/*.jar .

lcb:
	./gradlew lcbJar
	cp build/libs/*.jar .

clean:
	rm *.jar
	./gradlew clean

build: all

run: all
	./da_project
