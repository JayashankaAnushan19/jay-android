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
