// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** A listener on loopback that answers every request of the protocol with {@code answer}. */
final class FakeDaemon implements AutoCloseable {
  record Seen(String authorization, String body) {}

  final List<Seen> seen = new CopyOnWriteArrayList<>();
  private final HttpServer server;
  private final int status;

  FakeDaemon(int status, Function<String, String> answer) throws IOException {
    this.status = status;
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api",
        exchange -> {
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          seen.add(new Seen(exchange.getRequestHeaders().getFirst("Authorization"), body));
          byte[] out = answer.apply(body).getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(this.status, out.length);
          exchange.getResponseBody().write(out);
          exchange.close();
        });
    server.start();
  }

  URI base() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Override
  public void close() {
    server.stop(0);
  }

  static final String STATUS =
      "{\"type\":\"status\",\"pid\":1,\"startedAt\":\"t\",\"ruleset\":\"mine\",\"rules\":7,"
          + "\"modules\":[],\"pending\":0,\"protocol\":1}";
}
