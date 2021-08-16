import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestReactiveTest {


    @Test
    @DisplayName("testing Flux.interval expect NoEvent - Duration")
    public void test5(){

        Flux<Integer> interval = Flux.interval(Duration.ofSeconds(1))
                .map(Long::intValue);


        interval.subscribe(System.out::println);

        StepVerifier.withVirtualTime(() -> interval)
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext(0)
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext(1)
                .thenCancel()
                .verify();

    }


    @Test
    @DisplayName("testing Flux.interval then wait")
    public void test4(){

        Flux<Integer> interval = Flux.interval(Duration.ofSeconds(1))
                .map(Long::intValue)
                .take(2);

        interval.subscribe(System.out::println);

        StepVerifier.withVirtualTime(() -> interval)
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(2))
                .expectNextCount(4)
                .verifyComplete();

    }

    @Test
    @DisplayName("testing desconocido")
    public void test3(){

        Mono<String> stringMono = Mono.delay(Duration.ofMillis(3000))
                .map(d -> d.toString()+"Spring 4")
                .or(Mono.delay(Duration.ofMillis(2000))
                        .map(d -> d.toString()+" Spring 5"));

        stringMono.subscribe(System.out::println);

        StepVerifier.create(stringMono)
                .expectNext("0 Spring 5") // count 1

                .verifyComplete();
    }


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