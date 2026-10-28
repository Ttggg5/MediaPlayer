# Media Player

A modern Android media player for music and video, built with Kotlin, Jetpack Compose (Material 3 / Material You) and AndroidX Media3 (ExoPlayer).

## Features

**Music**
- Library scanned from the device (MediaStore): Songs, Albums, Artists and Folders tabs
- Search, plus sorting by title, artist, album, date added or duration (remembered between launches)
- Background playback with a media notification and lock-screen controls (`MediaSessionService`)
- Mini player and a full-screen "Now playing" screen with blurred artwork backdrop, seek bar, shuffle and repeat
- Queue: play next, add to queue, jump to, reorder and remove
- Playlists stored in a Room database: create, rename, delete, add and remove songs

**Split screen**
- Play a video and music at the same time: tap the split button in the video player
- Drag the divider to resize the panes (stacked in portrait, side by side in landscape)
- Separate volume sliders mix the video and the music; the music pane can browse songs and edit the queue

**Video**
- Thumbnail grid with duration and resolution, folder filter chips, search and sorting
- Full-screen player with custom Compose controls
- Gestures: swipe on the left for brightness, swipe on the right for volume, swipe sideways to seek, double-tap to skip ±10 s
- Audio track and subtitle selection, playback speed, fit/crop/stretch, rotation lock
- Shuffle and repeat (all / one), plus a "shuffle play" button for the whole video library
- Picture-in-picture (enters automatically on Android 12+ when you leave while playing)

**Settings & updates**
- Theme (system/light/dark) and dynamic colors
- Video gestures on/off, skip length, resume where you left off
- Choose which folders are scanned, separately for music and videos (subfolders included)
- Hide short audio clips, rescan library
- Built-in updater: checks GitHub Releases, downloads the new APK and opens the installer

**Streaming**
- Play HTTP(S) files and HLS (`.m3u8`) and DASH (`.mpd`) streams as video or audio
- Recent links and sample streams
- Opens audio and video files shared from other apps

## Tech stack

| Area | Library |
| --- | --- |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Playback | Media3 ExoPlayer (+ HLS, DASH), Media3 Session, Media3 UI |
| Storage | Room (playlists), DataStore (settings) |
| Images | Coil (album art and video thumbnails) |

minSdk 26 (Android 8.0), targetSdk 35.

## Building

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. GitHub Actions builds it on every push and uploads it as the `media-player-debug` artifact.

### Release builds and signing

Release APKs are signed with one fixed key so new versions install over old ones (and the in-app updater works). The key is never committed. CI reads it from these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | The `.jks` keystore, base64-encoded |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

With the secrets set, every push also uploads a signed `media-player-release` artifact.

Every push to the default branch is published automatically as a GitHub Release named `v1.0.<commit count>`, with the signed `MediaPlayer-<version>.apk` attached. The in-app updater offers the latest one. To bump the major/minor version, change `VERSION_BASE` in `.github/workflows/android.yml`.

For local release builds, create an untracked `keystore.properties` in the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, then run `./gradlew assembleRelease -PappVersion=1.0.1`.

## Project layout

```
app/src/main/java/com/ttggg5/mediaplayer/
├── data/          MediaStore scanning, Room playlists, DataStore settings
├── playback/      PlaybackService (MediaSessionService) and MusicController
└── ui/
    ├── home/      Main scaffold, bottom navigation, mini player host
    ├── music/     Library tabs and detail screens
    ├── nowplaying/ Mini player, now playing screen and queue
    ├── video/     Video grid, VideoPlayerActivity (gestures, PiP, tracks)
    ├── playlists/ Playlist list, dialogs
    ├── stream/    Network URL playback
    └── theme/     Material 3 theme with dynamic color
```
