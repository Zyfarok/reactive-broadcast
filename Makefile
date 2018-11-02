all: jar

jar:
	./gradlew makeJar
	cp build/libs/*.jar .

clean:
	rm *.jar
	./gradlew clean

build: all

