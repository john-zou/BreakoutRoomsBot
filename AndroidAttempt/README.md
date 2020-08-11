## Zoom Breakout Rooms Bot - Android SDK Attempt

These are the main files for the custom Zoom Breakout Rooms Management and chat response API that I worked on from about Aug 1 - Aug 10. The breakout rooms API seems to create breakout rooms that are function but work fundamentally differently from ones creatred by real Zoom Clients, producing unexpected results including splitting a large (130-person) meeting into multiple meetings, as detailed in `RealTests`. There are also [major limitations](https://devforum.zoom.us/t/breakout-room-problem/7658/15?u=john.zou) but this may change in the future.

This is no longer being actively pursued, as I have learned that developing a Chrome browser extension for a single TA or instructor to use, is by far the best solution. However, this source code may be useful when Zoom updates their SDK.

- `src` contains `BreakoutRoomBot.kt` and `TagParser.kt`, which holds the main code

- `UnitTests` is a separate and complete standalone Gradle project that I used for unit testing the logic of TagParser and Breakout Rooms assignment functions

- `RealTests` contains summary, analysis and logs of real tests

### Requirements
- Android Studio
- JDK 8+

### Instructions for running the App
1. Follow the [instructions here](https://marketplace.zoom.us/docs/sdk/native-sdks/android/getting-started/install-sdk)
    - Note: my code is based on SDK v5.0.24437.0708
    - Specific versions may be downloaded from their [GitHub repo](https://github.com/zoom/zoom-sdk-android/tags)
2. Copy paste the `src` folder here to `<Zoom SDK root>/mobilertc-android-studio/sample/src`
3. Enable Kotlin upon Android Studio prompt (add it to Gradle files if not)
4. As stated in the Zoom instructions, add JWT secret to AuthConstants

### Instructions for running the `UnitTests` project
1. Open `UnitTests` as a project and use `Gradle`.
    - In IntelliJ IDEA, it should just work without configuration.