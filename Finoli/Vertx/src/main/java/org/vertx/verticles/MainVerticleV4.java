package org.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticleV4 extends AbstractVerticle {

  private static final String DB_URL = "jdbc:mysql://localhost:3306/finoli";
  private static final String DB_USERNAME = "root";
  private static final String DB_PASSWORD = "123456";

  private SQLClient sqlClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    // Create a JDBC client
    sqlClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", DB_URL)
      .put("user", DB_USERNAME)
      .put("password", DB_PASSWORD)
      .put("driver_class", "com.mysql.jdbc.Driver"));

    // Create a router
    Router router = Router.router(vertx);

    // Define routes
    router.route(HttpMethod.GET, "/api/users").handler(this::handleGetUsers);
    router.route(HttpMethod.POST, "/api/users").handler(this::handleAddUser);

    // Start the HTTP server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080, result -> {
        if (result.succeeded()) {
          System.out.println("Server started on port 8080");
//          startPromise.complete();
        } else {
          System.out.println("Failed to start the server: " + result.cause());
          startPromise.fail(result.cause());
        }
      });
  }

  private void handleGetUsers(RoutingContext routingContext) {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.query("SELECT * FROM users", queryResult -> {
          if (queryResult.succeeded()) {
            routingContext.response()
              .putHeader("Content-Type", "application/json")
              .end(Json.encodePrettily(queryResult.result().getRows()));
          } else {
            routingContext.response()
              .setStatusCode(500)
              .end("Internal Server Error");
          }
          connection.close();
        });
      } else {
        routingContext.response()
          .setStatusCode(500)
          .end("Internal Server Error");
      }
    });
  }

  private void handleAddUser(RoutingContext routingContext) {
    // Get the user data from the request body
    JsonObject user = routingContext.getBodyAsJson();
    if (user == null) {
      routingContext.response()
        .setStatusCode(400)
        .end("Bad Request");
    } else {
      sqlClient.getConnection(res -> {
        if (res.succeeded()) {
          SQLConnection connection = res.result();
          connection.updateWithParams("INSERT INTO users (name, email) VALUES (?, ?)",
            new JsonArray().add(user.getString("name")).add(user.getString("email")),
            insertResult -> {
              if (insertResult.succeeded()) {
                routingContext.response()
                  .setStatusCode(201)
                  .end("User added successfully");
              } else {
                routingContext.response()
                  .setStatusCode(500)
                  .end("Internal Server Error");
              }
              connection.close();
            });
        } else {
          routingContext.response()
            .setStatusCode(500)
            .end("Internal Server Error");
        }
      });
    }
  }
}

