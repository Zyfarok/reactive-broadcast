all: 
	./gradlew fatJar
	cp build/libs/*.jar .

clean:
	rm *.jar
	./gradlew clean

build: all

run: all
	./da_project
