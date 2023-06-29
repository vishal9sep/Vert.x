package org.vertx.verticles;

import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainVerticleV3 extends AbstractVerticle {

  private MySQLPool mySQLPool;

  @Override
  public void start() {
    System.out.println("hello");
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // MySQL configuration
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setHost("localhost")
      .setPort(3306)
      .setDatabase("finoli")
      .setUser("root")
      .setPassword("123456");

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5); // Maximum pool size

    mySQLPool = MySQLPool.pool(vertx, connectOptions, poolOptions);

    // Create the user_info table if it doesn't exist
    mySQLPool.getConnection(connection -> {
      if (connection.succeeded()) {
        connection.result().query("CREATE TABLE IF NOT EXISTS user_info (id integer PRIMARY KEY, name VARCHAR(64), email VARCHAR(64), gender ENUM('male', 'female'), status ENUM('active', 'inactive'), timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);")
          .execute()
          .onSuccess(res -> System.out.println("user_info table created or already exists"))
          .onFailure(Throwable::printStackTrace);
      } else {
        connection.cause().printStackTrace();
      }
    });
    System.out.println("hello routes");
    // API routes
    router.post("/api/users").handler(this::createUser);
    router.get("/api/users").handler(this::getUsers);

    vertx.createHttpServer().requestHandler(router).listen(8888);

    System.out.println("Started on port 8888");
  }

  private void createUser(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();

    // Generate a new UUID
    String id = UUID.randomUUID().toString();

    // Prepare the INSERT query
    String query = "INSERT INTO user_info (id, name, email, gender, status) VALUES (?, ?, ?, ?, ?)";
    Tuple params = Tuple.of(id, user.getString("name"), user.getString("email"), user.getString("gender"), user.getString("status"));

    mySQLPool.withConnection(connection -> connection
      .preparedQuery(query)
      .execute(params)
      .onSuccess(res -> {
        JsonObject jsonResponse = new JsonObject().put("id", id);
        response.setStatusCode(201)
          .putHeader("Content-Type", "application/json")
          .end(jsonResponse.encode());
      })
      .onFailure(Throwable::printStackTrace)
    );
  }

  private void getUsers(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    MultiMap queryParams = routingContext.queryParams();

    // Prepare the SELECT query based on query parameters
    StringBuilder queryBuilder = new StringBuilder("SELECT * FROM user_info");
    Tuple params = Tuple.tuple();

    if (queryParams.contains("name")) {
      queryBuilder.append(" WHERE name = ?");
      params.addValue(queryParams.get("name"));
    }

    if (queryParams.contains("gender")) {
      if (queryParams.contains("name")) {
        queryBuilder.append(" AND gender = ?");
      } else {
        queryBuilder.append(" WHERE gender = ?");
      }
      params.addValue(queryParams.get("gender"));
    }

    if (queryParams.contains("status")) {
      if (queryParams.contains("name") || queryParams.contains("gender")) {
        queryBuilder.append(" AND status = ?");
      } else {
        queryBuilder.append(" WHERE status = ?");
      }
      params.addValue(queryParams.get("status"));
    }

    mySQLPool.withConnection(connection -> connection
      .preparedQuery(queryBuilder.toString())
      .execute(params)
      .onSuccess(res -> {
        List<JsonObject> userList = new ArrayList<>();
        for (Row row : res) {
          JsonObject user = new JsonObject()
            .put("id", row.getValue("id"))
            .put("name", row.getValue("name"))
            .put("email", row.getValue("email"))
            .put("gender", row.getValue("gender"))
            .put("status", row.getValue("status"));
          userList.add(user);
        }
        response.putHeader("Content-Type", "application/json")
          .end(Json.encode(userList));
      })
      .onFailure(Throwable::printStackTrace)
    );
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticleV3());
  }
}

