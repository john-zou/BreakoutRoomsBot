## Zoom Breakout Rooms Bot - Windows SDK Attempt

These are the main files for the custom Zoom Breakout Rooms Management and chat response API that I worked on from about Jul 24-Jul 31, when I switched to using the Android SDK.

While the interface and functions exist in the SDK, some major functions required for breakout rooms capabilities work:
- Creating breakout rooms
- Assigning users to breakout rooms
- Starting breakout rooms

### Requirements:
- Windows XP+ OS
- Windows 8 SDK (Windows 10 SDK untested)
- Visual Studio 2017 (VS2019 requires several additional configuration)
- VS Desktop C++ development package
- VS Toolchain v140 (v141 or v142 untested)
- [Boost](https://dl.bintray.com/boostorg/release/1.73.0/source/) (for logging and string functions)

### Instructions:

1.  Download [Zoom Windows SDK v5.0.24433.0616](https://github.com/zoom/zoom-sdk-windows)
2.  Copy the files in this repo folder to `<ZoomSDK root>/demo/sdk_demo_v2`
3.  In VS, Linker settings, change the path for Boost to where it is saved/built on your computer (build Boost for the appropriate VS Toolchain version)
4.  The project is only runnable in x86 Release mode due to SDK limitation
