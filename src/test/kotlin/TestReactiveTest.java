import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class TestReactiveTest {


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