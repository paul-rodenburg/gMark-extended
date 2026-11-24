#!/bin/bash

# Navigate to the directory containing this .command file
cd "$(dirname "$0")" || exit 1

# Build the JAR
./gradlew -PrefName=ev2.2 :client:shadowJar || exit 1

# Run the JAR
java -jar client/build/libs/gMark-vev2.2.jar
