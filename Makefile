all: 
	./gradlew build
	cp build/libs/*.jar .

clean:
	rm *.jar
	./gradlew clean

build: all