# Multiple Users

A demo to show how to support multiple users in one Service, ContentProvider, etc.

## Tips

- Please use release build for testing(to be granted permission)
- For emulator, better to use Android Automotive 14 devices(with support of multiple users)

## Verification

- Push release build to device or emulator(Android 13+)
  - `./gradlew app:build`
  - `adb shell mkdir system_ext/priv-app/MultiUsers`
  - `adb push app/build/outputs/apk/release/app-release.apk system_ext/priv-app/MultiUsers/MultiUsers.apk`
- Push permission white list of privileged application to device or emulator
  - `adb push io.github.okiele.users.xml  system_ext/etc/permissions/`
- Reboot device or emulator to test
  - `adb reboot`
  - Open application from a user other than SYSTEM
