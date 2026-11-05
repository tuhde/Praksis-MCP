// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import it.uhde.praksis.protocol.ProtocolException;
import it.uhde.praksis.protocol.Request;
import it.uhde.praksis.protocol.Response;
import it.uhde.praksis.protocol.Wire;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Semaphore;

/**
 * One connection to a daemon: {@code POST /api} with a bearer token and one request of the protocol
 * as JSON. The token stays in this object; it is never part of a message that an agent sees.
 */
public final class DaemonClient {
  private final URI endpoint;
  private final String token;

  /**
   * The daemon lets a token have eight requests at once and answers the rest with 429. An agent may
   * call tools in parallel, so the calls wait here; four leaves room for another client that uses
   * the same token.
   */
  static final int MAX_AT_ONCE = 4;

  private final Semaphore slots = new Semaphore(MAX_AT_ONCE, true);

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public DaemonClient(URI base, String token) {
    this.endpoint = base.resolve("/api");
    this.token = token;
  }

  /** Sends a request; a refusal or an error reply is a {@link DaemonException}. */
  public Response call(Request request) throws DaemonException {
    HttpResponse<String> answer;
    try {
      slots.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DaemonException("interrupted");
    }
    try {
      HttpRequest http =
          HttpRequest.newBuilder(endpoint)
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofString(Wire.encode(request), StandardCharsets.UTF_8))
              .build();
      answer = this.http.send(http, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DaemonException("interrupted");
    } catch (IOException | RuntimeException e) {
      throw new DaemonException("cannot reach the daemon at " + endpoint.resolve("/"));
    } finally {
      slots.release();
    }
    Response response;
    try {
      response = Wire.decodeResponse(answer.body());
    } catch (ProtocolException e) {
      throw new DaemonException(
          "the daemon answered " + answer.statusCode() + " without a reply of the protocol");
    }
    if (response instanceof Response.Error error) {
      throw new DaemonException(error.message());
    }
    if (answer.statusCode() / 100 != 2) {
      throw new DaemonException("the daemon answered " + answer.statusCode());
    }
    return response;
  }

  /** The JSON of a reply, as the daemon sent it. */
  public static String json(Response response) throws DaemonException {
    try {
      return Wire.encode(response);
    } catch (ProtocolException e) {
      throw new DaemonException("cannot show the reply");
    }
  }
}
