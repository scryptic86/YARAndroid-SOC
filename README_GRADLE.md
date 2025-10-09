# Setup and build

1. Install Android SDK and JDK 17+ (JDK 17 or later recommended).
2. Update `local.properties` with your SDK path, e.g. `sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk`.
3. From the repository root run Gradle wrapper (if you have it) or use your installed Gradle:

   ./gradlew assembleDebug

Notes

- The `app` module is configured to use your existing `src/` and `res/` directories and the top-level `AndroidManifest.xml`.
- If you get resource or API errors, you may need to update imports in code that used old support libraries to AndroidX.
- This repository has been migrated to AndroidX. The Gradle config now depends on `androidx.legacy:legacy-support-v4:1.0.0` instead of the old `android-support-v4.jar`.
- If you still have `libs/android-support-v4.jar` in the repo, you can safely remove it (or back it up) since Gradle will provide the AndroidX replacement.
