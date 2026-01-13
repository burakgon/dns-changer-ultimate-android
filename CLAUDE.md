# Project Notes for Claude

## Build Environment

Android Studio is installed via JetBrains Toolbox at:
`/Users/burakgon/Applications/Android Studio.app`

Java (JBR) bundled with Android Studio:
`/Users/burakgon/Applications/Android Studio.app/Contents/jbr/Contents/Home`

### Build Commands

To build the project:
```bash
export JAVA_HOME="/Users/burakgon/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

To install to connected device:
```bash
export JAVA_HOME="/Users/burakgon/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew installDebug
```

### ADB Location
ADB is typically at: `~/Library/Android/sdk/platform-tools/adb`
