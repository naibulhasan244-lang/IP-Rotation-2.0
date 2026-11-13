# IP Rotation 2.0
Developed by NiROB

Android app (Kotlin, Jetpack Compose, Hilt, Room, DataStore, OkHttp) that rotates through your
proxies on a timer, shows the current public IP + country + ping, logs every IP, and can show a
draggable floating overlay with the countdown and IP.

## Build
**Android Studio:** open this folder, wait for Gradle sync, press Run. Or
Build > Build Bundle(s) / APK(s) > Build APK(s).

**GitHub Actions (no PC):** upload this folder's contents to a GitHub repository (the file
`settings.gradle.kts` must be at the repository root). Open the Actions tab > "Build APK" > run it,
then download the APK from the run's Artifacts.

## Proxy formats
`ip:port`, `ip:port:user:pass`, `user:pass@ip:port`, `user:pass:ip:port`,
`http|https|socks4|socks5://ip:port`, `scheme://user:pass@ip:port`. One per line; `#` starts a comment.

## Important
When you tap START the app asks for VPN permission. It then creates a VPN whose system proxy is a tiny local
proxy (127.0.0.1) that forwards through the CURRENT rotation proxy, so the phone's browsers and other
proxy-aware apps show the rotated IP. Needs Android 10+. This is NOT a full tun2socks: UDP and apps that
ignore the system proxy are blocked, not tunneled.

HTTPS-type proxies are used as normal HTTP CONNECT proxies (TLS to the proxy itself is not supported).

## Change the package name
Replace `com.yourname.iprotation2` in `app/build.gradle.kts` (namespace, applicationId) and rename the
source folders / package lines to match (Android Studio: right-click package > Refactor > Rename).
