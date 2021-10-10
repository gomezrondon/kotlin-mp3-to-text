import com.sun.tools.javac.Main;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TestReactive {

    public static void main(String[] args) {


/*
        Mono<List<String>> listMono = Main2Kt.getListValues().collectList();
        listMono.flatMapMany(Flux::fromIterable).subscribe(System.out::println);
*/


        extracted(args);


    }

    private static void extracted(String[] args) {
        final long start = System.nanoTime();
        Mono.just(args)
                .map( x -> new Variables(x[0], x[1], 50))
                .flatMap( variables -> {
                    Main2Kt.resetFolders(variables.getAudioFilePath());
                    Main2Kt.extractWavFromMp4(variables.getVideoFilePath(), variables.getAudioFilePath());
                    Main2Kt.splitWavToChunk(variables.getAudioFilePath(), variables.getChunkSizeMs());
                    return Mono.just(variables);
                })
                .flatMap( variables -> {
                    List<File> files = Main2Kt.readAudioChunksFiles();
                    variables.setList(files);
                    return Mono.just(variables);
                }).flatMap( variables -> {
                    // procesamiento de cada audio chunk
                    return   generateScriptsChunks(variables)
                            .then(Mono.just(variables));
                }).flatMap( variables -> {
                    Main2Kt.combineTextFiles(variables.getChunkSizeMs(), variables.getList().size());
                    return Mono.just("done!");
                })
                .doFinally(endType -> System.out.println("Time taken : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " milliseconds."))
                .subscribe(System.out::println);
    }

    @NotNull
    private static Flux<Mono<Object>> generateScriptsChunks(Variables variables) {
        return Flux.fromIterable(variables.getList())
                .map(x -> {
                    String nameWithouExtenxion = Main2Kt.getNameWithouExtenxion(x);
                    return new genDto(nameWithouExtenxion, x.getPath());
                })
                .map(x -> new genDto("text_chunks/"+x.getNewName()+".txt", x.getWavPath()))
                .map(x -> {
                    File textNewFile = new File(x.getNewName());
                    String output = Main2Kt.speechRecognitionOffline(x.getWavPath());
                    Main2Kt.writeToFile(textNewFile, output);
                    return Mono.empty();
                });
    }


}
