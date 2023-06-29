package org.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.vertx.verticles.MainVerticle;

public class Main {
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }


}