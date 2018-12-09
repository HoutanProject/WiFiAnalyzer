#!/bin/sh
mkdir -p $(pwd)/caches/

if [ -z "$1" ]; then
	COMMAND="gradle build"
else
	COMMAND="$1"
fi

#-v $(pwd)/caches/android-sdk:/home/user/android-sdk-linux \
# -u $(id -u):$(id -g) \
docker run -it \
	-w /home/user/project \
	-v $(pwd):/home/user/project \
	-v $(pwd)/caches/gradle-cache:/home/user/.gradle \
	-v $(pwd)/caches/android-cache:/home/user/.android \
	houtan/android-sdk $COMMAND
