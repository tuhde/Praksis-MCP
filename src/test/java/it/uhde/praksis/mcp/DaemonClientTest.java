// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.uhde.praksis.protocol.Request;
import it.uhde.praksis.protocol.Response;
import java.net.URI;
import org.junit.jupiter.api.Test;

class DaemonClientTest {
  @Test
  void postsARequestOfTheProtocolWithTheToken() throws Exception {
    try (var daemon = new FakeDaemon(200, b -> FakeDaemon.STATUS)) {
      Response r = new DaemonClient(daemon.base(), "pwt_x").call(new Request.Status());
      assertThat(r).isInstanceOf(Response.StatusReport.class);
      assertThat(daemon.seen).hasSize(1);
      assertThat(daemon.seen.get(0).body()).isEqualTo("{\"op\":\"status\"}");
      assertThat(daemon.seen.get(0).authorization()).isEqualTo("Bearer pwt_x");
    }
  }

  @Test
  void anErrorReplyKeepsTheWordsOfTheDaemon() throws Exception {
    try (var daemon =
        new FakeDaemon(403, b -> "{\"type\":\"error\",\"message\":\"this token is read only\"}")) {
      assertThatThrownBy(() -> new DaemonClient(daemon.base(), "pwt_x").call(new Request.Status()))
          .isInstanceOf(DaemonException.class)
          .hasMessage("this token is read only");
    }
  }

  @Test
  void somethingThatIsNotAReplyIsNotShownAsOne() throws Exception {
    try (var daemon = new FakeDaemon(502, b -> "<html>bad gateway pwt_x</html>")) {
      assertThatThrownBy(() -> new DaemonClient(daemon.base(), "pwt_x").call(new Request.Status()))
          .isInstanceOf(DaemonException.class)
          .hasMessageContaining("502")
          .message()
          .doesNotContain("pwt_x")
          .doesNotContain("html");
    }
  }

  @Test
  void anUnreachableDaemonIsAnErrorThatDoesNotShowTheToken() {
    assertThatThrownBy(
            () ->
                new DaemonClient(URI.create("http://127.0.0.1:1"), "pwt_x")
                    .call(new Request.Status()))
        .isInstanceOf(DaemonException.class)
        .hasMessageContaining("cannot reach")
        .message()
        .doesNotContain("pwt_x");
  }

  @Test
  void parallelCallsWaitForASlotSoThatTheDaemonLimitIsNotReached() throws Exception {
    try (var daemon = new FakeDaemon(200, b -> FakeDaemon.STATUS)) {
      daemon.delayMillis = 60;
      var client = new DaemonClient(daemon.base(), "t");
      try (var pool = java.util.concurrent.Executors.newFixedThreadPool(16)) {
        var futures = new java.util.ArrayList<java.util.concurrent.Future<Response>>();
        for (int i = 0; i < 24; i++) {
          futures.add(pool.submit(() -> client.call(new Request.Status())));
        }
        for (var f : futures) {
          assertThat(f.get()).isInstanceOf(Response.StatusReport.class);
        }
      }
      assertThat(daemon.seen).hasSize(24);
      assertThat(daemon.mostAtOnce.get()).isBetween(2, DaemonClient.MAX_AT_ONCE);
    }
  }
}
