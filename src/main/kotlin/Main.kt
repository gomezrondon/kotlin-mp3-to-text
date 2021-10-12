import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioSystem

/**
 * param1 option
 * param2 video path
 * java -jar kotlin-mp3-to-text.jar 1 C:\temp\video.mp4
 */
 suspend fun main(args: Array<String>) {

    val option = args[0]

    when (option) {
        "1" -> {
            val videoFilePath = args[1]
            transcribeMp4Video(videoFilePath)
        }
        "2" -> {
            val videoFilePath = args[1]
            val chunkSizeMs = args[2].toInt()
            resetFolder("video_chunks")
            splitMp4ToChunk(videoFilePath, chunkSizeMs)
        }
    }

}

private suspend fun transcribeMp4Video(videoFilePath: String) {
    val audioFilePath = "audio.wav"

    resetFolders(audioFilePath)

    //Extract WAV audio from Mp4 Video
    extractWavFromMp4(videoFilePath, audioFilePath)

    //Split Wav file into chunks
    val chunkSizeMs = 50 // 50 seconds
    splitWavToChunk(audioFilePath, chunkSizeMs)

    val audioFolder = readAudioChunksFiles()

    convertWavToText2(audioFolder)

    combineTextFiles(chunkSizeMs, audioFolder.size)
//    combineTextFiles(50, 201)
}

public fun readAudioChunksFiles() = File("audio_chunks").walkTopDown().filter { it.isFile }.toList()


public fun splitMp4ToChunk(videoFilePaht: String, chunkSizeMs: Int) {
    "cmd.exe /c ffmpeg -i $videoFilePaht -f segment -segment_time $chunkSizeMs video_chunks/chunk%d.mp4".runCommand(
        timeout = 0,
        outPutFile = "salida.txt"
    )
}

public fun splitWavToChunk(audioFilePath: String, chunkSizeMs: Int) {
    "cmd.exe /c ffmpeg -i $audioFilePath -f segment -segment_time $chunkSizeMs audio_chunks/chunk%d.wav".runCommand(
        timeout = 120,
        outPutFile = "salida.txt"
    )
}



public fun extractWavFromMp4(videoFilePath: String, audioFilePath: String) {
    "cmd.exe /c ffmpeg -i $videoFilePath -b:a 96K -vn $audioFilePath".runCommand(
        timeout = 120,
        outPutFile = "salida.txt"
    )
}

data class Variables(val videoFilePath: String, val audioFilePath:String, var chunkSizeMs:Int=0){
    var list:MutableList<File> = mutableListOf()
}


/**
 * Reset working folders and delete transcription file
 */
public fun resetFolders(audioFilePath: String) {

    deleteFile(audioFilePath)
    deleteFile("transcription.txt")

    resetFolder("text_chunks")
    resetFolder("audio_chunks")
    resetFolder("video_chunks")

}

private fun deleteFile(audioFilePath: String) {
    if (File(audioFilePath).exists()) {
        File(audioFilePath).delete()
    }
}

private fun resetFolder(pathname: String) {
    if (File(pathname).exists()) {
        File(pathname).deleteRecursively()
    }
    File(pathname).mkdir()
}

/**
 * Convert a list of wav files to text files
 */
suspend fun convertWavToText2(audioFolder: List<File>) {

     val indexed = audioFolder.map {  file ->
         GlobalScope.async {
             generateScript(file)
         }
     }

     indexed.awaitAll()

 }


fun convertWavToText3(audioFolder: List<File>) {
    audioFolder.map {  file ->
        generateScript(file)
    }

}

public fun getNameWithouExtenxion(file: File): String {
    return file.nameWithoutExtension
}




data class genDto(val newName: String, val wavPath: String)

private fun generateScript(file: File) {
    val output = speechRecognitionOffline(file.path)
    val fileName = file.nameWithoutExtension
    val fullPahtFile = "text_chunks/$fileName.txt"
    val textNewFile = File(fullPahtFile)
    if (textNewFile.createNewFile()) {
        writeToFile(textNewFile, output)
    }
}

public fun writeToFile(textNewFile: File, output: String) {
    textNewFile.appendText(output + "\n")
}

public fun combineTextFiles(chunkSizeMs: Int, totalFiles: Int) {
    var dateTime = LocalTime.of(0, 0, 1)
    (0..totalFiles).forEach {
        if (File("text_chunks/chunk$it.txt").exists()) {
            val textLine = File("text_chunks/chunk$it.txt").readText()
            File("transcription.txt").appendText(dateTime.toString() + "|\n " + textLine)
        }

        dateTime = dateTime.plusSeconds(chunkSizeMs.toLong())
    }
}


fun speechRecognitionOffline(audioFile:String): String {
    LibVosk.setLogLevel(LogLevel.DEBUG)

    Model("model").use { model ->
        AudioSystem.getAudioInputStream(BufferedInputStream(FileInputStream(audioFile)))
            .use { ais ->
                Recognizer(
                    model,
                    96000F
                ).use { recognizer ->  //96000 es bueno //44100 is but does not work //128kH CD quality
                    var nbytes: Int
                    val b = ByteArray(4096)
                    while (ais.read(b).also { nbytes = it } >= 0) {
                        if (recognizer.acceptWaveForm(b, nbytes)) {
                            //   System.out.println(recognizer.getResult());
                        }
                    }
                    return cleanOutputRecognizedText(recognizer.finalResult)
                }
            }
    }
}

fun cleanOutputRecognizedText(result: String): String {
/*
    val test = """
         {
          "text" : "he comes across speedy and other m p's in the bathroom she tells him to go to sleep when he refuses they tried to subdue him but he fights back and manages to escape meanwhile steve takes marty and andy to a warehouse and tell them to wait for him while he calls for help as a duplicate continue to search for them states needs into a building and finds himself in an office he tries using the phone but he hears someone else using it when he enters another room he finds major collins trying to connect to an outside line however the woman on the other end tells collins but all the lines are busy at the moment commons gets hysterical when the woman calls him by his name even though he never mentioned it he throws the phone against the wall and take some it's enemies when steve approaches him the major points a gun at steve telling him to stay back steve asked him if he has a car but the major argues that it's too late to run college tells them that they have to stand up and fight because of duplicate will just come after them if they run collins offers him some instead of means"
        }
""".trimIndent()*/

   return result.split("\n") .toList().drop(1).dropLast(1)
        .map { it.replace(""""text" : """","") }
        .map { it.removeRange(it.length-1,it.length) }
        .flatMap { it.split(" ").chunked(35) }
        .map { it.joinToString(" ") }
        //.forEach { println(it) }
        .joinToString("\n")

}



fun String.runCommand(workingDir: File? = null, timeout:Long, outPutFile:String) {
    deleteFile(outPutFile)

    val process = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.appendTo(File(outPutFile)))
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    // File(outPutFile).readLines().forEach { println(it) }

    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
        process.destroy()
        throw RuntimeException("execution timed out: $this")
    }
    if (process.exitValue() != 0) {
        throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
    }


}

