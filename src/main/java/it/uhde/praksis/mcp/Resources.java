// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import it.uhde.praksis.protocol.Request;
import java.util.List;

/**
 * The resources: the documentation pages and the rules in use, so that a client can read them
 * without a tool call. Everything is read from the daemon when it is asked for, so the server
 * starts even when the daemon is down.
 */
final class Resources {
  private Resources() {}

  static final String HELP_INDEX = "praksis://help";

  /**
   * The matcher of the SDK lets a variable stand for one path segment, so the pages of the
   * documentation (one or two segments, like {@code user/rules}) get one template for each depth.
   */
  static final List<String> HELP_TEMPLATES =
      List.of("praksis://help/{page}", "praksis://help/{section}/{page}");

  static final String EFFECTIVE = "praksis://effective";

  static List<SyncResourceSpecification> fixed(DaemonClient daemon) {
    return List.of(
        new SyncResourceSpecification(
            McpSchema.Resource.builder(HELP_INDEX, "help")
                .title("Praksis documentation: the pages")
                .description("The list of the pages of the documentation, with their topics.")
                .mimeType("text/plain")
                .build(),
            (exchange, request) -> read(request.uri(), "text/plain", () -> helpText(daemon, null))),
        new SyncResourceSpecification(
            McpSchema.Resource.builder(EFFECTIVE, "effective")
                .title("The rules in use")
                .description("Every rule of the ruleset in use with the layer it comes from.")
                .mimeType("application/json")
                .build(),
            (exchange, request) ->
                read(
                    request.uri(),
                    "application/json",
                    () -> DaemonClient.json(daemon.call(new Request.Effective())))));
  }

  static List<SyncResourceTemplateSpecification> templates(DaemonClient daemon) {
    return HELP_TEMPLATES.stream()
        .map(
            template ->
                new SyncResourceTemplateSpecification(
                    McpSchema.ResourceTemplate.builder(template, "help-page")
                        .title("Praksis documentation: a page")
                        .description(
                            "One page of the documentation in Markdown, for example"
                                + " praksis://help/user/rules. The rule format and the layers are"
                                + " explained there.")
                        .mimeType("text/markdown")
                        .build(),
                    (exchange, request) -> {
                      String uri = request.uri();
                      String topic =
                          uri.startsWith(HELP_INDEX + "/")
                              ? uri.substring(HELP_INDEX.length() + 1)
                              : "";
                      return read(uri, "text/markdown", () -> helpText(daemon, topic));
                    }))
        .toList();
  }

  private interface Text {
    String get() throws DaemonException;
  }

  private static String helpText(DaemonClient daemon, String topic) throws DaemonException {
    return ReadTools.helpText(daemon.call(new Request.Help(topic)));
  }

  private static McpSchema.ReadResourceResult read(String uri, String mime, Text text) {
    String body;
    try {
      body = text.get();
    } catch (DaemonException e) {
      throw new IllegalStateException(e.getMessage());
    }
    return McpSchema.ReadResourceResult.builder(
            List.of(McpSchema.TextResourceContents.builder(uri, body).mimeType(mime).build()))
        .build();
  }
}
