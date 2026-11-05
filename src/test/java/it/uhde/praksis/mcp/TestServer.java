// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

/** Like {@link Main}, but with a tool that starts a command added, to test the modes. */
public final class TestServer {
  private TestServer() {}

  public static void main(String[] args) {
    Config config = Config.from(System.getenv());
    var daemon = new DaemonClient(config.base(), config.token());
    var server = McpApp.start(daemon, config.mode(), ToolsTest.withWriter(), System.in, System.out);
    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
  }
}
