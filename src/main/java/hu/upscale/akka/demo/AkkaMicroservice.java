package hu.upscale.akka.demo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import hu.upscale.akka.demo.guice.GuiceInjectionModule;

/**
 * @author László Zoltán
 */
public interface AkkaMicroservice {

    static void main(String[] args) {
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new GuiceInjectionModule());
        AkkaHttpServer akkaHttpServer = injector.getInstance(AkkaHttpServer.class);

        akkaHttpServer.start();
    }

}
