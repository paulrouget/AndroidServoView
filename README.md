ServoView is a drop-in replacement for GeckoView on Android.

To build the dynamic library (libservoglue.so): `./rust/android/build.sh`

To build `geckoview.aar`: `./gradlew assemble`

Note: The archive and the classes are still named "GeckoXXX" eventhough it uses Servo.

To test with Firefox Klar:

- clone https://github.com/mozilla-mobile/focus-android/
- Point to the Servo's GeckoView:

``` diff
diff --git a/app/build.gradle b/app/build.gradle
index 186a9b36..204b230b 100644
--- a/app/build.gradle
+++ b/app/build.gradle
@@ -267,6 +267,10 @@ repositories {
         // x86 GeckoView builds
         url "https://index.taskcluster.net/v1/task/gecko.v2.mozilla-central.nightly.2018.03.07.latest.mobile.android-x86-opt/artifacts/public/android/maven"
     }
+    flatDir(
+        name: 'localBuild',
+        dirs: '/Users/paul/git/ServoView/geckoview/build/outputs/aar/'
+    )
 }

 dependencies {
@@ -295,8 +299,12 @@ dependencies {
     // What I actually want to define here is geckoArmCompile and geckoX86Compile. But gradle doesn't
     // allow me to define dependencies for such a flavor combination. However as we are not building
     // WebView + (ARM / x86) flavor combinations this should be good enough for now.
-    armCompile "org.mozilla:geckoview-nightly-armeabi-v7a:60.0a1"
-    x86Compile "org.mozilla:geckoview-nightly-x86:60.0a1"
+    // armCompile "org.mozilla:geckoview-nightly-armeabi-v7a:60.0a1"
+    // x86Compile "org.mozilla:geckoview-nightly-x86:60.0a1"
+    armCompile (
+            name: 'geckoview-release',
+            ext: 'aar'
+    )

     testCompile 'junit:junit:4.12'
     testCompile "org.robolectric:robolectric:3.7.1"
diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml
index 2f8e364d..2163877f 100644
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -8,6 +8,7 @@

     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
+    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
     <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />

     <application
```

- build and run on device: `./gradlew installKlarGeckoArmDebug`

**Important**: push the Servo resources directory to the sdcard, and make sure in the Android settings that Servo has read access to the sdcard.
