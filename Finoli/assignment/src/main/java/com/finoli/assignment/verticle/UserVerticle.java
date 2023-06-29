package com.finoli.assignment.verticle;

import com.finoli.assignment.entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;

public class UserVerticle extends AbstractVerticle {

  private JDBCClient jdbcClient;

  @Override
  public void start(Promise<Void> startPromise){

    final JsonObject config = new JsonObject()
      .put("url", "jdbc:mysql://localhost:3306/finoli")
      .put("user", "root")
      .put("password", "123456");

    jdbcClient = JDBCClient.create(vertx, config); // tried with create method

    Router router = Router.router(vertx);  //Router instance

    /*
     * @Post Method to Add Users
     * This method take User`s Json Request body in Input
     * Store the User`s data into user_info table in the DB.
     */

    router.post("/api/users").handler(BodyHandler.create()).handler(routingContext -> {

      JsonObject json =routingContext.getBodyAsJson();
      User user = new User(
        json.getInteger("id"),
        json.getString("name"),
        json.getString("email"),
        json.getString("gender"),
        json.getString("status"),
        LocalDateTime.now());

      // Prepare the insert query
      String insertQuery = "INSERT INTO user_info (id, name, email, gender, status, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

      // Bind the values
      JsonArray values = new JsonArray()
        .add(user.getId().toString())
        .add(user.getName())
        .add(user.getEmail())
        .add(user.getGender())
        .add(user.getStatus())
        .add(user.getTimestamp());

      jdbcClient.getConnection(connectionResult -> {
        if (connectionResult.succeeded()) {
          SQLConnection connection = connectionResult.result();

          connection.queryWithParams(insertQuery, values, insertResult -> {
            if (insertResult.succeeded()) {
              System.out.println("User inserted successfully.");
            } else {
              System.out.println("Failed to 12 insert user: " + insertResult.cause().getMessage());
            }
            connection.close(); // Closed JDBC Connection
          });
        } else {
          System.out.println("Failed to obtain a database connection: " + connectionResult.cause().getMessage());
        }
      });
      JsonObject response = new JsonObject();
      response.put("message", "Succedfully add the User into DB");
      routingContext.response().end(response.encode());
    });







    /*
     * @Get Method to get all Users List and we can filter users with Query parameter
     */

    router.get("/api/users").handler(routingContext -> {

      // All the Query parameter will be he
      MultiMap queryParams = routingContext.queryParams();

      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM user_info");

      if (queryParams.contains("name")) {
        queryBuilder.append(" WHERE name = '").append(queryParams.get("name")).append("'");
      }
      if (queryParams.contains("gender")) {
        if (queryParams.contains("name")) {
          queryBuilder.append(" AND gender ='").append(queryParams.get("gender")).append("'");
        } else {
          queryBuilder.append(" WHERE gender ='").append(queryParams.get("gender")).append("'");
        }
      }

      if (queryParams.contains("status")) {
        if (queryParams.contains("name") || queryParams.contains("gender"))
          queryBuilder.append(" AND status ='").append(queryParams.get("status")).append("'");
        else
          queryBuilder.append(" WHERE status = '").append(queryParams.get("status")).append("'");
      }

      jdbcClient.getConnection(connectionResult -> {
        if (connectionResult.succeeded()) {
          SQLConnection connection = connectionResult.result();
          String selectQuery = queryBuilder.toString();

          JsonArray jsonArray = new JsonArray();

          connection.query(selectQuery, queryResult -> {
            if (queryResult.succeeded()) {
              queryResult.result().getRows().forEach(row -> {

                JsonObject json = new JsonObject();
                json.put("id",row.getInteger("id"));
                json.put("name",row.getString("name"));
                json.put("email",row.getString("email"));
                json.put("gender",row.getString("gender"));
                json.put("status",row.getString("status"));
                json.put("timestamp",row.getString("timestamp"));

                jsonArray.add(json);

              });
              routingContext.response().end(jsonArray.encodePrettily());

              System.out.println("end of GET");
            } else {
              System.out.println("Failed to insert user: " +queryResult.cause().getMessage());
            }
            // Close the connection
            connection.close();
          });
        } else {
          // Failed to obtain a connection
          System.out.println("Failed to obtain a database connection: " + connectionResult.cause().getMessage());
        }
      });
    });

    vertx.createHttpServer().requestHandler(router).listen(8888);
  }
}
