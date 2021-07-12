import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis


val PYTHON_PATH = "C:\\temp\\test\\pyMp3ToText\\"

suspend fun main(args: Array<String>) {

    val mp3FileName = args[0]

    if (File("transcription.txt").exists()) {
        File("transcription.txt").delete()
    }

    val chunkSizeMs = 10000
    "cmd.exe /c python ${PYTHON_PATH}mp3_to_wavs.py $mp3FileName $chunkSizeMs".runCommand(timeout = 120,outPutFile = "salida.txt")

    val audioFolder = File("audio_chunks").walkTopDown().filter { it.isFile }.toList()
    convertWavToText(audioFolder)

    val seconds = TimeUnit.MILLISECONDS.toSeconds(chunkSizeMs.toLong())
    var dateTime =  LocalTime.of(0, 0, 1)
    (audioFolder.indices).forEach {
        val textLine = File("text_chunks/chunk$it.txt").readText()

        File("transcription.txt").appendText(dateTime.toString() + "| "+textLine)

        dateTime = dateTime.plusSeconds(seconds)
    }

}

suspend  fun convertWavToText(audioFolder: List<File>) {
    val time = measureTimeMillis {
        val map = audioFolder.map { file ->
            GlobalScope.async {
                "cmd.exe /c python ${PYTHON_PATH}convert_wav_to_text.py $file".runCommand(
                    timeout = 150,
                    outPutFile = "salida.txt"
                )
            }
        }
         map.awaitAll()
    }
    println("total time C: $time")
}




fun String.runCommand(workingDir: File? = null, timeout:Long, outPutFile:String) {
    if (File(outPutFile).exists()) {
        File(outPutFile).delete()
    }
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