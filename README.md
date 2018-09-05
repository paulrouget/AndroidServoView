A drop-in replacement for GeckoView. For now, compatible with Crow.

## Build instructions:

- `cd servoview`
- `wget https://download.servo.org/nightly/android/servo-latest.aar`
- `cd ..`
- `./gradlew assemble`
- aar is available under: `geckoview/build/outputs/aar`

## Compile Crow with the ServoView:

- clone https://github.com/paulrouget/FirefoxReality/ (check last commit to see the diff with upstream).
- create a `user.properties`, and specify the path to the aar: `geckoViewLocal={PATH_TO}/geckoview/build/outputs/aar/geckoview-release.aar`
- `cd servoview`
- `wget https://download.servo.org/nightly/android/servo-latest.aar`
- build with Android Studio

## Limitations

It's still a basic integration. Controller should work to scroll and click. Keyboard, tabs and `resource://` urls are not supported yet.
