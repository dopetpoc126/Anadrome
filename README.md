# Optimus Prime Live Wallpaper

![Wallpaper preview](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper/blob/main/assets/live_wallpaper.gif?raw=true)


A dynamic Android Live Wallpaper featuring Optimus Prime, built using Kotlin and the latest ExoPlayer (Media3) library. This wallpaper intelligently manages video playback based on device state (visibility, screen lock, and battery saver mode) to provide a smooth, engaging experience while being mindful of system resources.

* **Immersive Video Playback:** Experience a high-quality, full-screen Optimus Prime video loop on your home and lock screens.
* **Intelligent Resource Management:**
    * **Visibility-Aware:** Automatically pauses video playback when your home screen is not visible (e.g., when an app is open), saving battery.
    * **Screen Lock/Unlock:** Video intelligently pauses and resets to the beginning when your screen locks, resuming seamlessly when unlocked.
    * **Battery Saver Mode Integration:** Automatically pauses playback when your device enters Battery Saver mode to conserve power.
* **Seamless Preview Experience:** A dedicated button within the app allows you to preview the live wallpaper directly before applying it.
* **Static First Frame Fallback:** Ensures a smooth transition and prevents black screens by displaying a static image (`first_frame.png`) until the video player is fully rendered.
* **Muted by Design:** The video plays without audio, as typical for live wallpapers.
* **Optimal Scaling:** Video content is automatically scaled and cropped to perfectly fit your device's screen.

## Get Started

### For Users (Apply the Live Wallpaper):

1.  **Download the APK:**
    * Head over to the [Releases](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper/releases) section of this repository.
    * Download the latest `Primus.apk` file.
2.  **Install the APK:**
    * Transfer the downloaded `.apk` file to your Android device.
    * You might need to enable "Install from Unknown Sources" in your device's security settings to install the app.
3.  **Launch the App and Apply:**
    * After installation, open the "Optimus Prime Live Wallpaper" app from your app drawer.
    * Tap the **"Apply Primus Wallpaper"** button.
    * Your device's live wallpaper picker will open. Select "Optimus Prime Live Wallpaper" (or "Primus") from the list and confirm.
    * **Optional:** Use the **"Preview Wallpaper"** button in the app to see the video in action before applying!

### For Developers (Build from Source):

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper.git](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper.git)
    ```
2.  **Open in Android Studio:**
    * Launch Android Studio.
    * Go to `File > Open`, then navigate to the cloned `Optimus-Prime-Live-Wallpaper` directory and open it.
3.  **Sync Gradle Project:**
    * Android Studio should automatically sync the Gradle project. If not, click `File > Sync Project with Gradle Files`.
4.  **Add Your Video and First Frame:**
    * Place your desired Optimus Prime video file named `my_wallpaper_video.mp4` into the `app/src/main/res/raw/` directory.
    * Place a corresponding static image named `first_frame.png` (a screenshot of the first frame of your video works well) into the `app/src/main/res/drawable/` directory.
5.  **Run on Device/Emulator:**
    * Connect an Android device or start an emulator.
    * Click the `Run` button (green play icon) in Android Studio to build and install the app.
    * Once installed, follow the "For Users" instructions above to apply the wallpaper.

## Project Structure

* `app/src/main/java/com/example/livewallpaper/VideoWallpaperService.kt`: The core Live Wallpaper service logic. Handles ExoPlayer setup for the wallpaper, drawing to the surface, and reacting to device state changes (screen lock, battery saver, visibility).
* `app/src/main/java/com/example/livewallpaper/MainActivity.kt`: The primary activity. Provides the user interface to apply the wallpaper and includes a **new in-app video preview** using Jetpack Compose and ExoPlayer.
* `app/src/main/res/raw/my_wallpaper_video.mp4`: **The main video file.**.
* `app/src/main/res/drawable/first_frame.png`: **The static image** used as a fallback.
* `app/src/main/AndroidManifest.xml`: Declares the `WallpaperService` and `MainActivity`, along with necessary permissions.
* `app/build.gradle`: Manages project dependencies, including ExoPlayer (Media3) and Jetpack Compose.

## How It Works (Technical Details)

The project leverages two main components:

1.  **`VideoWallpaperService` (Live Wallpaper Core):**
    * Extends `android.service.wallpaper.WallpaperService` to create a live wallpaper engine.
    * Manages an `ExoPlayer` instance that renders directly to the wallpaper's `SurfaceHolder`.
    * Utilizes `BroadcastReceiver`s to listen for `ACTION_USER_PRESENT`, `ACTION_SCREEN_OFF`, and `ACTION_POWER_SAVE_MODE_CHANGED` to control video playback efficiently.
    * Implements `Player.Listener` callbacks to detect when the video first renders (`onRenderedFirstFrame`), allowing a smooth transition from the static `first_frame.png`.

2.  **`MainActivity` (User Interface):**
    * Built with **Jetpack Compose** for a modern Android UI.
    * Provides a button to directly launch the live wallpaper picker with the correct component.
    * Introduces a **`VideoPlayerComposable`** that embeds another `ExoPlayer` instance to play `my_wallpaper_video.mp4` directly within the app, offering a real-time preview of the wallpaper. This preview player is lifecycle-aware, pausing when the app is in the background and releasing resources when no longer needed.

This architecture ensures both robust wallpaper functionality and a user-friendly application experience.

## Contributing

Contributions are warmly welcomed! If you have suggestions, find bugs, or want to add new features, please feel free to:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/my-awesome-feature`).
3.  Make your changes and test them thoroughly.
4.  Commit your changes (`git commit -m 'feat: Add a new awesome feature'`).
5.  Push to the branch (`git push origin feature/my-awesome-feature`).
6.  Open a Pull Request describing your changes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Disclaimer:** This is a fan-made project and is not affiliated with, endorsed by, or sponsored by Hasbro, Paramount Pictures, or any other entities associated with the Transformers franchise. Optimus Prime and all related characters are trademarks of their respective owners.
