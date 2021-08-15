import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class TestReactive {

    public static void main(String[] args) {

        Mono.just(args)
                .log(">>>> start process >>> ")
                .map( x -> new Variables(x[0], x[1], 50))
                .flatMap( variables -> {
                    Main2Kt.resetFolders(variables.getAudioFilePath());
                    return Mono.just(variables);
                }).flatMap( variables -> {
                    Main2Kt.extractWavFromMp4(variables.getVideoFilePath(), variables.getAudioFilePath());
                    return Mono.just(variables);
                }).flatMap( variables -> {
                    Main2Kt.splitWavToChunk(variables.getAudioFilePath(), variables.getChunkSizeMs());
                    return Mono.just(variables);
                }).flatMap( variables -> {
                    List<File> files = Main2Kt.readAudioChunksFiles();
                    variables.setList(files);
                    return Mono.just(variables);
                }).flatMap( variables -> {
                     Main2Kt.convertWavToText3(variables.getList());
                    return Mono.just(variables);
                }).flatMap( variables -> {
                    Main2Kt.combineTextFiles(variables.getChunkSizeMs(), variables.getList());
                    return Mono.just("done!");
                })
                .log(">>>> End process >>> ")
                .subscribe(System.out::println);



    }

}
