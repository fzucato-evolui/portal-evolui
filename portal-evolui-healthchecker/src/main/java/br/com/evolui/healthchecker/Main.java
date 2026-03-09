package br.com.evolui.healthchecker;

import br.com.evolui.healthchecker.controller.MainController;

import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Main {
    public static void main(String[] args) {

        try {
            Future<?> future = newSingleThreadExecutor().submit(new MainController(args));
            future.get();
        } catch (Throwable ex) {
            //logger.severe(UtilFunctions.exceptionToString(ex));
            ex.printStackTrace();
            System.exit(-1);
        }

    }
}
