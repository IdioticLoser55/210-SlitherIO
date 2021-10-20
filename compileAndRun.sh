#!/bin/bash

javac -cp .:lib/Java-WebSocket-1.5.2.jar:lib/darcula.jar:lib/slf4j-api-1.7.32.jar:lib/slf4j-simple-1.7.32.jar -d build src/main/java/de/mat2095/my_slither/*.java
if [ $? -eq 0 ]; then
    java -cp .:lib/Java-WebSocket-1.5.2.jar:lib/slf4j-api-1.7.32.jar:lib/slf4j-simple-1.7.32.jar:build de/mat2095/my_slither/Main
fi

