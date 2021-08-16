import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestReactiveTest {


    @Test
    @DisplayName("testing converting from mono list to flux")
    public void testingGetListValues(){
        Mono<List<String>> listMono = Main2Kt.getListValues().collectList();
        Flux<String> stringFlux = listMono.flatMapMany(Flux::fromIterable);

        stringFlux.subscribe(System.out::println);


        StepVerifier.create(stringFlux)
                .expectNext("a") // count 1
                .expectNext("javier")// count 2
                .expectNextCount(1) // count 3

                .verifyComplete();
    }

    @Test
    public void test2(){
        File file = new File("audio_chunks/chunk0.wav");
        Mono<String> log = Main2Kt.generateScriptMono(Mono.just(file))
                .log();
        log.subscribe();


        StepVerifier.create(log)
                .expectNext("chunk0")
                .verifyComplete();
    }

}