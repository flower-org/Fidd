package com.fidd.data.utils;

import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class DBTool {

    public static CompletableFuture<Long> getLastInsertedIdAsync(SqlConnection connection) {
        CompletableFuture<Long> resultFuture = new CompletableFuture<>();

        connection.query("SELECT LAST_INSERT_ID();").execute()
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        Long id = null;
                        for (Row row : rows) {
                            id = row.getLong(0);
                            break;
                        }
                        resultFuture.complete(id);
                    } else {
                        resultFuture.completeExceptionally(ar.cause());
                    }
                });

        return resultFuture;
    }

    public static <T> T connectGetResultAndClose(Pool pool, Function<SqlConnection, CompletableFuture<T>> fn) throws ExecutionException, InterruptedException {
        return connectGetResultAndCloseAsync(pool, fn).get();
    }

    public static <T> CompletableFuture<T> connectGetResultAndCloseAsync(Pool pool, Function<SqlConnection, CompletableFuture<T>> fn) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        try {
            pool.getConnection().onComplete(connectionResult -> {
                if (connectionResult.succeeded()) {
                    try {
                        SqlConnection connection = connectionResult.result();
                        getResultAndCloseAsync(connection, fn).whenComplete((res, err) -> {
                            if (err != null) {
                                resultFuture.completeExceptionally(err);
                            } else {
                                resultFuture.complete(res);
                            }
                        });
                    } catch (Exception e) {
                        resultFuture.completeExceptionally(e);
                    }
                } else {
                    resultFuture.completeExceptionally(connectionResult.cause());
                }
            });
        } catch (Exception e) {
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }

    public static <T> T getResultAndClose(SqlConnection connection, Function<SqlConnection, CompletableFuture<T>> fn) throws ExecutionException, InterruptedException {
        return getResultAndCloseAsync(connection, fn).get();
    }

    public static <T> CompletableFuture<T> getResultAndCloseAsync(SqlConnection connection, Function<SqlConnection, CompletableFuture<T>> fn) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();

        try {
            fn.apply(connection).whenComplete((fnResult, err) -> {
                connection.close();
                if (err != null) {
                    resultFuture.completeExceptionally(err);
                } else {
                    resultFuture.complete(fnResult);
                }
            });
        } catch (Exception e) {
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }

    public static <T> T connectCommitAndClose(Pool pool, Function<SqlConnection, CompletableFuture<T>> fn) throws ExecutionException, InterruptedException {
        return connectCommitAndCloseAsync(pool, fn).get();
    }

    public static <T> CompletableFuture<T> connectCommitAndCloseAsync(Pool pool, Function<SqlConnection, CompletableFuture<T>> fn) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        try {
            pool.getConnection().onComplete(connectionResult -> {
                if (connectionResult.succeeded()) {
                    try {
                        SqlConnection connection = connectionResult.result();
                        commitAndCloseAsync(connection, fn).whenComplete((res, err) -> {
                            if (err != null) {
                                resultFuture.completeExceptionally(err);
                            } else {
                                resultFuture.complete(res);
                            }
                        });
                    } catch (Exception e) {
                        resultFuture.completeExceptionally(e);
                    }
                } else {
                    resultFuture.completeExceptionally(connectionResult.cause());
                }
            });
        } catch (Exception e) {
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }

    public static <T> T commitAndClose(SqlConnection connection, Function<SqlConnection, CompletableFuture<T>> fn) throws ExecutionException, InterruptedException {
        return commitAndCloseAsync(connection, fn).get();
    }

    public static <T> CompletableFuture<T> commitAndCloseAsync(SqlConnection connection, Function<SqlConnection, CompletableFuture<T>> fn) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();

        try {
            fn.apply(connection).whenComplete((fnResult, err) -> {
                if (err != null) {
                    connection.close();
                    resultFuture.completeExceptionally(err);
                } else {
                    connection.query("COMMIT").execute().onComplete(commitResult -> {
                        if (commitResult.succeeded()) {
                            resultFuture.complete(fnResult);
                        } else {
                            Throwable cause = commitResult.cause();
                            if (cause != null && cause.getMessage() != null && cause.getMessage().contains("Not in transaction")) {
                                resultFuture.complete(fnResult);
                            } else {
                                resultFuture.completeExceptionally(cause != null ? cause : new RuntimeException("Commit failed"));
                            }
                        }
                        connection.close();
                    });
                }
            });
        } catch (Exception e) {
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }
}
