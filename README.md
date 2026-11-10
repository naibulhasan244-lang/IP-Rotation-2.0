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
Proxies are applied to the app's own network checks (that is how the IP shown is verified).
Other apps on the phone are NOT routed through the proxies. Whole-phone routing needs a
VpnService + tun2socks layer, which is not included.

HTTPS-type proxies are used as normal HTTP CONNECT proxies (TLS to the proxy itself is not supported).

## Change the package name
Replace `com.yourname.iprotation2` in `app/build.gradle.kts` (namespace, applicationId) and rename the
source folders / package lines to match (Android Studio: right-click package > Refactor > Rename).
