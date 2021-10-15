#!/bin/bash

javac -cp .:lib/Java-WebSocket-1.5.2.jar -d build src/main/java/de/mat2095/my_slither/*.java
if [ $? -eq 0 ]; then
    java -cp build de/mat2095/my_slither/Main
fi

