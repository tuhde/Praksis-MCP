// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

/** Starts the server on standard input and output; the log goes to standard error. */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    Config config;
    try {
      config = Config.from(System.getenv());
    } catch (IllegalArgumentException e) {
      // stdout belongs to the protocol; the reason goes to stderr
      System.err.println("praksis-mcp: " + e.getMessage());
      System.exit(2);
      return;
    }
    var server =
        McpApp.start(
            new DaemonClient(config.base(), config.token()),
            config.mode(),
            Tools.catalog(),
            System.in,
            System.out);
    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
  }
}
