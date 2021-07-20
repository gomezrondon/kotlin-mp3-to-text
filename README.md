# kotlin-mp3-to-text
Using Kotlin to manage the transcription of mp3 to text

### 1) You need to install the ffmpeg library:
https://github.com/BtbN/FFmpeg-Builds/releases 
download the file: ffmpeg-N-103011-g6f20685228-win64-gpl.zip  (unzip it)

### 2) add the ffmpeg\bin path to your classpath (on windows)

### 2.1) download the machine learning model 
https://alphacephei.com/vosk/models "vosk-model-en-us-aspire-0.2" 1.4 GB in size.
unzip it and change its folder name to just "model"

### 3) clone this repoistory

### 4) cd to the repository
    4.1) copy the "model" folder into the repository

### 5) execute:
 ```
gradle run --args="'C:\temp\video.mp4' audio.wav"
 ```
 
 ### 6) the output will be a "Transcription.txt" file.
