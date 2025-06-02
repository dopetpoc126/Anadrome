# Optimus Prime Live Wallpaper

![Wallpaper preview]([https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper/blob/main/first_frame_preview.png](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper/blob/main/assets/live_wallpaper.gif)?raw=true)


A dynamic Android Live Wallpaper featuring Optimus Prime, built using Kotlin and the latest ExoPlayer (Media3) library. This wallpaper intelligently manages video playback based on device state (visibility, screen lock, and battery saver mode) to provide a smooth, engaging experience while being mindful of system resources.

## ✨ Features

* **Optimized Video Playback:** Utilizes ExoPlayer (Media3) for efficient and high-performance video rendering.
* **Intelligent State Management:**
    * **Visibility-Aware:** Pauses video when the wallpaper is not visible (e.g., when an app is open).
    * **Screen Lock/Unlock:** Automatically pauses the video and resets to the first frame on screen lock, resuming playback when the device is unlocked.
    * **Battery Saver Mode:** Automatically pauses video playback when Battery Saver mode is active to conserve power, resuming when it's disabled.
* **Static First Frame Fallback:** Displays a static image (`first_frame.png`) as a placeholder until the video player is ready to render, ensuring a seamless transition and preventing black screens.
* **Muted Playback:** Video plays without audio, as typical for live wallpapers.
* **Configurable Scaling:** Video content is scaled to fit the screen with cropping to fill the entire wallpaper surface.

## 🛠️ Technologies Used

* **Kotlin:** The primary programming language for Android development.
* **Android SDK:** Core Android framework.
* **ExoPlayer (Media3):** Google's open-source media player library for high-performance video playback.
* **Gradle:** Build automation tool.

## 🚀 Installation & Usage

### For Users (Applying the Live Wallpaper):

1.  **Download the APK:**
    * Go to the [Releases](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper/releases) section of this GitHub repository.
    * Download the latest `.apk` file.
2.  **Install the APK:**
    * Transfer the downloaded `.apk` file to your Android device.
    * Enable "Install from Unknown Sources" in your device settings (if prompted) to install the app.
3.  **Apply as Live Wallpaper:**
    * **Option A (Recommended):** After installation, open your device's `Settings` app. Search for "Wallpaper" or "Live Wallpaper."
    * **Option B:** Long-press on your home screen, select "Wallpapers & style" (or similar), then navigate to "Live Wallpapers."
    * Select "Optimus Prime Live Wallpaper" from the list and apply it.

### For Developers (Building from Source):

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper.git](https://github.com/dopetpoc126/Optimus-Prime-Live-Wallpaper.git)
    ```
2.  **Open in Android Studio:**
    * Launch Android Studio.
    * Select `File > Open`, then navigate to the cloned repository folder and open it.
3.  **Sync Gradle:**
    * Android Studio should automatically sync the Gradle project. If not, click `File > Sync Project with Gradle Files`.
4.  **Run on Device/Emulator:**
    * Connect an Android device or start an emulator.
    * Click the `Run` button (green play icon) in Android Studio.
    * After the app is installed, follow the "For Users" instructions above to apply the wallpaper.

## 📁 Project Structure

* `app/src/main/java/com/example/livewallpaper/VideoWallpaperService.kt`: Contains the core logic for the live wallpaper service, including ExoPlayer initialization, state management, and drawing.
* `app/src/main/res/raw/my_wallpaper_video.mp4`: The video file for Optimus Prime. **Make sure your desired video is named `my_wallpaper_video.mp4` and placed here.**
* `app/src/main/res/drawable/first_frame.png`: The static image displayed as a fallback before video playback starts. **Ensure this image exists.**
* `app/src/main/AndroidManifest.xml`: Declares the wallpaper service and necessary permissions.
* `app/build.gradle`: Defines project dependencies (including ExoPlayer Media3).

## 💡 How it Works

The `VideoWallpaperService` extends Android's `WallpaperService` and manages the lifecycle of the live wallpaper. The `VideoWallpaperEngine` nested class handles:

* **Surface Management:** Interacts with the `SurfaceHolder` to draw video frames.
* **ExoPlayer Lifecycle:** Initializes, prepares, and releases the `ExoPlayer` instance.
* **Broadcast Receivers:** Listens for `ACTION_USER_PRESENT` (device unlock), `ACTION_SCREEN_OFF` (screen locked), and `ACTION_POWER_SAVE_MODE_CHANGED` to adjust video playback.
* **Playback Logic:** The `updateVideoPlayback()` method is the heart of the control, deciding whether the video should play based on visibility, phone lock status, and battery saver mode.
* **First Frame Handling:** The `onRenderedFirstFrame()` callback from ExoPlayer is used to detect when the video has started rendering, allowing the app to transition from the static `first_frame.png` to the live video.

## 🤝 Contributing

Contributions are welcome! If you find a bug or have an idea for an improvement, please:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature`).
3.  Make your changes.
4.  Commit your changes (`git commit -m 'Add new feature'`).
5.  Push to the branch (`git push origin feature/your-feature`).
6.  Open a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Disclaimer:** This is a fan-made project and is not affiliated with, endorsed by, or sponsored by Hasbro, Paramount Pictures, or any other entities associated with the Transformers franchise. Optimus Prime and all related characters are trademarks of their respective owners.
