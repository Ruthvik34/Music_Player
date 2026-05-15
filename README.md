# Music_Player

Simple Android music player built with Kotlin and ExoPlayer \(Media3\). Features include local audio playback, playlist support, shuffle (plays a random song immediately), repeat, double\-tap skip \(\+10s / \-10s\), and correct lifecycle handling so playback stops when closing the player.

## Features
- Play local audio files from device media store
- Playlists and queue management
- Shuffle: click shuffle to play a random song immediately
- Repeat single track toggle
- Double\-tap right/left on album art to skip forward/back by 10 seconds
- Player stops playback when leaving `PlayerActivity`

## Tech stack
- Kotlin
- ExoPlayer / AndroidX Media3
- Gradle / Android Studio

## Prerequisites
- `Android Studio` (Windows)
- JDK 11\+
- Android SDK (matching project `compileSdk`/`targetSdk`)
- A device or emulator with audio files or proper media permissions

## Setup & build
1. Clone the repo:
   ```bash
   git clone https://github.com/Ruthvik34/<your-repo>.git
   cd <your-repo>

