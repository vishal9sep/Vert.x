package com.finoli.assignment;

import com.finoli.assignment.verticle.UserVerticle;
import io.vertx.core.Vertx;

public class Application {
  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new UserVerticle());
  }
}
