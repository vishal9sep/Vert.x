package org.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    private static final int PORT = 8080;

    private JDBCPool pool;

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // API endpoints
        router.post("/api/users").handler(this::createUser);
        router.get("/api/users").handler(this::getUsers);



        // Create MySQL client
//        final JsonObject config = new JsonObject()
//                .put("jdbcUrl", "com.mysql.cj.jdbc.Driver")
//                .put("datasourceName", "jdbc:mysql://localhost:3306/finoli")
//                .put("username", "root")
//                .put("password", "123456")
//                .put("max_pool_size", 5);
//
//        pool = JDBCPool.pool(vertx, config);
        System.out.println("in start before HTTP server");
        // Start the HTTP server
        vertx.createHttpServer().requestHandler(router).listen(PORT, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                System.out.println("HTTP server started on port " + PORT);
            } else {
                System.out.println("HTTP failed");
                startPromise.fail(http.cause());
            }
        });
    }

    private void createUser(RoutingContext routingContext) {

        HttpServerResponse response = routingContext.response();
        JsonObject requestBody = routingContext.getBodyAsJson();
        String name = requestBody.getString("name");
        String email = requestBody.getString("email");
        String gender = requestBody.getString("gender");
        String status = requestBody.getString("status");

        String query = "INSERT INTO user_info (id, name, email, gender, status, timestamp) " +
                "VALUES (3, ?, ?, ?, ?, NOW())";

            pool.query(query)
                .execute()
                .onFailure(e -> {
                    System.out.println(e.getMessage());
                })
                .onSuccess(rows -> {
                    for (Row row : rows) {
                        System.out.println(row.getString("FIRST_NAME"));
                    }
                });
    }

    private void getUsers(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        List<String> conditions = new ArrayList<>();
        List<Tuple> params = new ArrayList<>();

        String name = routingContext.request().getParam("name");
        if (name != null) {
            conditions.add("name = ?");
            params.add(Tuple.of(name));
        }

        String gender = routingContext.request().getParam("gender");
        if (gender != null) {
            conditions.add("gender = ?");
            params.add(Tuple.of(gender));
        }

        String status = routingContext.request().getParam("status");
        if (status != null) {
            conditions.add("status = ?");
            params.add(Tuple.of(status));
        }

        String whereClause = "";
        if (!conditions.isEmpty()) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }

        String query = "SELECT * FROM user_info" + whereClause;

        pool.preparedQuery(query)
                .execute(ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> result = ar.result();
                        List<User> users = new ArrayList<>();
                        for (Row row : result) {
                            String id = row.getString("id");
                            String userName = row.getString("name");
                            String email = row.getString("email");
                            String userGender = row.getString("gender");
                            String userStatus = row.getString("status");
                            LocalDateTime timestamp = row.getOffsetDateTime("timestamp").toLocalDateTime();
                            users.add(new User(id, userName, email, userGender, userStatus, timestamp));
                        }
                        response.putHeader("content-type", "application/json")
                                .end(users.toString());
                    } else {
                        response.setStatusCode(500).end();
                    }
                });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    private static class User {
        private final String id;
        private final String name;
        private final String email;
        private final String gender;
        private final String status;
        private final LocalDateTime timestamp;

        public User(String id, String name, String email, String gender, String status, LocalDateTime timestamp) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.gender = gender;
            this.status = status;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = timestamp.format(formatter);
            return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"email\":\"" + email +
                    "\",\"gender\":\"" + gender + "\",\"status\":\"" + status + "\",\"timestamp\":\"" + formattedTimestamp + "\"}";
        }
    }
}
