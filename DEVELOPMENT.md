# Development

## Developing Using Docker

Benefits:

* no need to modify your environment
* can try new dependencies by changing the Dockerfile
* use any text editor to maintaining

Downsides:

* slow
* no nice IDE
* some challenges keeping image and host directory in sync

Pull the docker image, run it and run ```gradle build```:

```
./build-on-docker.sh
```

First time builds takes about 20 minutes (downloading dependencies, compilation and testing). 

Caches are stored in the ```caches/``` directory of the host.

Edit-compiled-run is done as:

```
gradle --continuous assembleDebug
```

You can install as follows (```brew install android-platform-tools```):

```
adb install -r -g app-debug.apk
```