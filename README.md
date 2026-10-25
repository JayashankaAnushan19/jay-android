<p align="center">
  <img src="docs/icon.png" alt="Jay app icon" width="120" />
</p>

<h1 align="center">Jay</h1>

<p align="center">
  A lightweight Android app that brings my portfolio, projects, blogs and favourite websites together in one place.
</p>

---

## About

**Jay** is a personal website launcher for Android. Instead of remembering links, you open the app, tap a card, and the site opens inside the app. Each site gets a branded loading screen, and the app shows no browser bars or web addresses.

Everything on the home screen is managed from a password-protected **admin panel** inside the app. You can add, edit, hide, reorder or remove websites without changing code or releasing a new APK.

The default list starts with my GitHub portfolio: [github.com/JayashankaAnushan19](https://github.com/JayashankaAnushan19).

## Features

**For everyone**
- Clean home screen with a greeting header and animated cards (icon, name and an optional short description)
- Websites open inside the app (Android WebView) with mobile-friendly layout where the site supports it
- Branded opening screen while a page loads, instead of a browser-style loading bar
- Pull down to refresh, a refresh button, and back navigation through visited pages
- Friendly offline and error screens with **Retry**
- Share link, copy link, and *Open in browser* in the ⋮ menu, for anything that doesn't work inside the app (e.g. Google sign-in)
- Phone, email, maps and app links open in the right app, and downloads go to the browser
- File uploads and fullscreen video work inside the app
- Light and dark mode, plus a splash screen on Android 12+

**Admin panel (Settings ⚙)**
- Password protected, with a salted PBKDF2-SHA256 hash and a 30-second lockout after 5 wrong attempts
- Add an unlimited number of websites
- Edit a website's name, description and URL
- Show or hide a website with a switch
- Drag to reorder
- Delete, with **Undo**
- Choose each website's icon from:
  - 🖼️ **Gallery**: any photo from the phone
  - 🙂 **Avatar**: 14 built-in icons in 8 colours
  - 🌐 **Site icon**: the website's own logo
  - 🔗 **Image link**: any image URL
  - 🔤 **Letter**: a coloured tile with the first letter of the name
- Change the admin password
- Export or import the website list as a JSON backup file
- Locks itself again when you leave the app

Everything is saved on the device and stays after closing the app or restarting the phone.

## Install

1. Download `Jay.apk` from the [Releases](https://github.com/JayashankaAnushan19/jay-android/releases) page.
2. Open it on your Android phone. If asked, allow *Install unknown apps* for your browser or file manager.
3. Open **Jay**.

Requires **Android 8.0 (API 26) or newer**.

## First-time setup

1. Tap ⚙ in the top-right corner of the home screen.
2. Log in with the default password **`admin`**.
3. Tap **Change password** and choose your own password.
4. Use **Add website** to add your sites. They appear on the home screen straight away.

> **Note:** websites and settings are stored on each phone separately. To copy your list to another phone, use **⋮ → Export websites** on one phone and **Import websites** on the other. Gallery photos aren't included in the backup.

## Build from source

**Requirements**
- JDK 17 or newer
- Android SDK with platform **35** and build-tools (Android Studio installs these for you)

**With Android Studio:** open the project folder and press **Run**.

**From the command line:**

```bash
# Tell Gradle where the Android SDK is (or set ANDROID_HOME)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

### Signed release build (optional)

Create a keystore and a `keystore.properties` file in the project root. Both are git-ignored, so they never get committed.

```properties
storeFile=jay-release.jks
storePassword=your-store-password
keyAlias=jay
keyPassword=your-key-password
```

Then run `./gradlew assembleRelease`. The release build is shrunk with R8 and signed with your key. Keep the keystore safe, because you need the same key for every future update.

## Making changes and building a new APK

> You only need to change code to change how the app **works or looks**. Adding, editing or removing websites is done inside the app (Settings ⚙) and needs no new APK.

### 1. Find what to edit

| I want to change… | Edit this |
|---|---|
| Texts (titles, greetings, messages, button labels) | `app/src/main/res/values/strings.xml` |
| Colours (light / dark mode) | `res/values/colors.xml` / `res/values-night/colors.xml` |
| Home screen header and layout | `res/layout/activity_main.xml` + `ui/MainActivity.kt` |
| Website cards on the home screen | `res/layout/item_website.xml` |
| In-app viewer (loading screen, errors, menu) | `res/layout/activity_webview.xml` + `ui/WebViewActivity.kt` |
| Admin panel and the add/edit website form | `ui/SettingsActivity.kt` + `res/layout/dialog_website.xml` |
| Built-in avatars and their colours | `ui/Avatars.kt` (+ an icon file in `res/drawable/`) |
| Websites a fresh install starts with | `seedDefaults()` in `data/WebsiteRepository.kt` |
| Default admin password, lockout rules | `data/AdminAuth.kt` |
| App icon | `res/mipmap-*/` images and `res/drawable/ic_launcher_background.xml` |
| App name | `app_name` in `res/values/strings.xml` |

### 2. Bump the version

In `app/build.gradle.kts`, increase both numbers:

```kotlin
versionCode = 4        // whole number, +1 every release
versionName = "1.3"    // the version people see
```

Android only installs the new APK over the old one if `versionCode` is higher.

### 3. Build the APK

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

You can also click **Build → Build App Bundle(s) / APK(s) → Build APK(s)** in Android Studio.

The new APK is created at `app/build/outputs/apk/debug/app-debug.apk`. If the build fails, the error message names the file and line to fix.

### 4. Install and test it

- Copy the APK to your phone and open it. It installs as an update, and saved websites and the password stay.
- Or, with the phone plugged in and USB debugging turned on: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- If the app ever crashes, the next launch shows the error details with a **Copy** button.

> **Keep using the same signing key.** Android only accepts an update if it's signed with the same key as the installed app. Debug builds use the debug key on the computer that built them, so an APK built on a different computer won't install over the old one. Either uninstall first (this erases the saved websites, so export them first) or use a [release key](#signed-release-build-optional) for every build.

### 5. Publish it

```bash
git add .
git commit -m "Describe what you changed"
git push
```

Then create a new GitHub **Release** (tag e.g. `v1.3`) and attach the APK.

## Project structure

```
app/src/main/
├── java/com/jay/launcher/
│   ├── JayApp.kt                 # Saves crash details and shows them on next launch
│   ├── data/
│   │   ├── Website.kt            # Website model (name, url, icon, description, enabled)
│   │   ├── WebsiteRepository.kt  # Saves the list (JSON in SharedPreferences), import/export
│   │   └── AdminAuth.kt          # Password hashing, verification and lockout
│   ├── ui/
│   │   ├── MainActivity.kt       # Home screen
│   │   ├── WebViewActivity.kt    # In-app viewer: loading, errors, refresh, external links
│   │   ├── SettingsActivity.kt   # Admin panel: login, manage sites, icons, password, backup
│   │   ├── SiteIcon.kt           # Draws a website's icon tile
│   │   └── Avatars.kt            # Built-in avatar icons and colours
│   └── util/
│       ├── Urls.kt               # URL checks, connectivity, open externally
│       ├── IconLoader.kt         # Small cached image loader (no extra library)
│       └── IconStore.kt          # Saves gallery photos as app icons
└── res/                          # Layouts, icons, themes, strings, animations
```

## Tech

- Kotlin, Android Views with View Binding
- Material Components (Material 3)
- AndroidX: AppCompat, RecyclerView, SwipeRefreshLayout
- No backend, no analytics, no third-party image or network libraries

## Author

**Jayashanka Anushan**, [GitHub @JayashankaAnushan19](https://github.com/JayashankaAnushan19)
