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

 suspend fun main(args: Array<String>) {

     val audioFilePath = args[0]

     resetFolders()

     val chunkSizeMs = 50000 // 50 seconds
    "cmd.exe /c python ${PYTHON_PATH}split_wavs.py $audioFilePath $chunkSizeMs".runCommand(timeout = 120,outPutFile = "salida.txt")

    val audioFolder = File("audio_chunks").walkTopDown().filter { it.isFile }.toList()

    convertWavToText2(audioFolder)

    combineTextFiles(chunkSizeMs, audioFolder)

}

private fun resetFolders() {
    if (File("text_chunks").exists()) {
        File("text_chunks").deleteRecursively()
    }
    File("text_chunks").mkdir()

    if (File("audio_chunks").exists()) {
        File("audio_chunks").deleteRecursively()
    }
    File("audio_chunks").mkdir()

    if (File("transcription.txt").exists()) {
        File("transcription.txt").delete()
    }
}

suspend fun convertWavToText2(audioFolder: List<File>) {

     val indexed = audioFolder.map {  file ->
         GlobalScope.async {
             val output = speechRecognitionOffline(file.path)
             val fileName = file.nameWithoutExtension
             if (File("text_chunks/$fileName.txt").createNewFile()) {
                 File("text_chunks/$fileName.txt").appendText(output + "\n")
             }
         }
     }

     indexed.awaitAll()

 }


private fun combineTextFiles(chunkSizeMs: Int, audioFolder: List<File>) {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(chunkSizeMs.toLong())
    var dateTime = LocalTime.of(0, 0, 1)
    (audioFolder.indices).forEach {
        val textLine = File("text_chunks/chunk$it.txt").readText()

        File("transcription.txt").appendText(dateTime.toString() + "|\n " + textLine)

        dateTime = dateTime.plusSeconds(seconds)
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
                        } else {
                            //   System.out.println(recognizer.getPartialResult());
                        }
                    }
                    //  println(recognizer.finalResult)
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