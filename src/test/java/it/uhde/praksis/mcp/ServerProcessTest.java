// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The real program in its own process, spoken to by a real MCP client. */
class ServerProcessTest {
  private static final String JAVA =
      Path.of(System.getProperty("java.home"), "bin", "java").toString();

  private static McpSyncClient client(String main, String url, String mode) {
    ServerParameters.Builder p =
        ServerParameters.builder(JAVA)
            .args("-cp", System.getProperty("java.class.path"), main)
            .addEnvVar("PRAKSIS_TOKEN", "pwt_x")
            .addEnvVar("PRAKSIS_URL", url);
    if (mode != null) {
      p.addEnvVar("PRAKSIS_MODE", mode);
    }
    var transport =
        new StdioClientTransport(p.build(), new JacksonMcpJsonMapper(new ObjectMapper()));
    McpSyncClient c = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(20)).build();
    c.initialize();
    return c;
  }

  private static List<String> names(McpSyncClient c) {
    return c.listTools().tools().stream().map(McpSchema.Tool::name).toList();
  }

  private static McpSchema.CallToolRequest call(String name) {
    return McpSchema.CallToolRequest.builder(name).arguments(Map.of()).build();
  }

  private static String text(McpSchema.CallToolResult r) {
    return ((McpSchema.TextContent) r.content().get(0)).text();
  }

  @Test
  void aToolCallReachesTheDaemonAndBringsTheReplyBack() throws Exception {
    try (var daemon = new FakeDaemon(200, b -> FakeDaemon.STATUS);
        McpSyncClient c = client(Main.class.getName(), daemon.base().toString(), null)) {
      assertThat(names(c)).containsExactly("praksis_status");
      McpSchema.CallToolResult r = c.callTool(call("praksis_status"));
      assertThat(r.isError()).isFalse();
      assertThat(text(r)).contains("\"rules\":7");
      assertThat(daemon.seen).hasSize(1);
      assertThat(daemon.seen.get(0).body()).isEqualTo("{\"op\":\"status\"}");
      assertThat(daemon.seen.get(0).authorization()).isEqualTo("Bearer pwt_x");
    }
  }

  @Test
  void readOnlyHidesTheCommandToolAndCommandsShowsIt() throws Exception {
    try (var daemon = new FakeDaemon(200, b -> FakeDaemon.STATUS)) {
      try (McpSyncClient c =
          client(TestServer.class.getName(), daemon.base().toString(), "read-only")) {
        assertThat(names(c)).contains("praksis_status").doesNotContain("praksis_run_command");
        // a tool that is not offered cannot be called by name either
        assertThatThrownBy(() -> c.callTool(call("praksis_run_command")))
            .hasMessageContaining("Unknown tool");
      }
      try (McpSyncClient c =
          client(TestServer.class.getName(), daemon.base().toString(), "commands")) {
        assertThat(names(c)).contains("praksis_status", "praksis_run_command");
        assertThat(text(c.callTool(call("praksis_run_command")))).isEqualTo("started");
      }
      assertThat(daemon.seen).isEmpty();
    }
  }

  @Test
  void aRefusalOfTheDaemonIsAToolErrorWithItsWordsAndWithoutTheToken() throws Exception {
    try (var daemon =
            new FakeDaemon(
                403, b -> "{\"type\":\"error\",\"message\":\"this token is read only\"}");
        McpSyncClient c = client(Main.class.getName(), daemon.base().toString(), null)) {
      McpSchema.CallToolResult r = c.callTool(call("praksis_status"));
      assertThat(r.isError()).isTrue();
      assertThat(text(r)).isEqualTo("this token is read only").doesNotContain("pwt_x");
    }
  }

  @Test
  void withoutATokenTheProgramSaysWhyOnStandardErrorAndWritesNothingToStandardOut()
      throws IOException, InterruptedException {
    Process p =
        new ProcessBuilder(JAVA, "-cp", System.getProperty("java.class.path"), Main.class.getName())
            .redirectErrorStream(false)
            .start();
    p.getOutputStream().close();
    String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(p.waitFor()).isEqualTo(2);
    assertThat(err).contains("PRAKSIS_TOKEN");
    assertThat(out).isEmpty();
  }
}
