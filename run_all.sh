#!/bin/bash

#for file in images/*.bmp; do
for file in images/titanic.bmp; do
    image_name=${file%.bmp}
    image_name=${image_name#images/}
    for method in LSB1 LSB4 LSBI; do
        ./gradlew run \
            --console=plain --quiet \
            --args="-extract -p ../images/${image_name}.bmp -out ../messages/extracted-${image_name}-${method}.txt -steg $method"
    done
done
