package org.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public class DemoVerticle extends AbstractVerticle {

    List<T>

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Router router = Router.router(vertx);

        router.get("/api/users").handler(context -> {
            context.request().response().end
        })
    }
}
