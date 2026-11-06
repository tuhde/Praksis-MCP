// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProposeToolsTest {
  private static final String ANSWER = "{\"type\":\"inspection\",\"data\":{\"id\":\"s1\"}}";

  private static Tool tool(String name) {
    return Tools.catalog().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
  }

  private static String sent(String name, Map<String, Object> args) throws Exception {
    try (var daemon = new FakeDaemon(200, b -> ANSWER)) {
      String out = tool(name).run(new DaemonClient(daemon.base(), "t"), args);
      assertThat(out).contains("\"id\":\"s1\"");
      assertThat(daemon.seen).hasSize(1);
      return daemon.seen.get(0).body();
    }
  }

  @Test
  void theProposalToolsAreProposalsAndOnlyThose() {
    assertThat(Tools.catalog().stream().filter(t -> t.kind() == Tool.Kind.PROPOSE))
        .extracting(Tool::name)
        .containsExactlyInAnyOrder(
            "praksis_agent_propose", "praksis_plan_propose", "praksis_notes_save");
  }

  @Test
  void testFilesAreStagedForARule() throws Exception {
    String body =
        sent(
            "praksis_agent_propose",
            Map.of(
                "layer",
                "mine",
                "rule",
                "mail.10-spam",
                "files",
                List.of(Map.of("path", "tests/mail.10-spam.toml", "content", "x = 1")),
                "reason",
                "no test yet"));
    assertThat(body)
        .startsWith("{\"op\":\"stage\",\"what\":\"agent.propose\"")
        .contains("\"layer\":\"mine\"")
        .contains("\"path\":\"tests/mail.10-spam.toml\"")
        .contains("\"reason\":\"no test yet\"")
        // the client name is not ours to choose: the web gate sets it
        .doesNotContain("\"client\"");
  }

  @Test
  void aPlanAndANoteAreHandedOver() throws Exception {
    assertThat(
            sent(
                "praksis_plan_propose",
                Map.of(
                    "request",
                    "move the invoices",
                    "steps",
                    List.of(Map.of("capability", "mail.move", "args", Map.of("to", "Invoices"))),
                    "context",
                    "I read 3 mails")))
        .contains("\"what\":\"plan.propose\"")
        .contains("\"capability\":\"mail.move\"")
        .contains("\"context\":\"I read 3 mails\"");
    assertThat(sent("praksis_notes_save", Map.of("title", "Spam rule", "text", "It does x.")))
        .contains("\"what\":\"notes.save\"")
        .contains("\"title\":\"Spam rule\"");
  }

  @Test
  void badArgumentsAreRefusedBeforeAnythingIsSent() throws Exception {
    List<Object[]> bad =
        List.of(
            new Object[] {"praksis_agent_propose", Map.of("layer", "m", "rule", "r")},
            new Object[] {
              "praksis_agent_propose", Map.of("layer", "m", "rule", "r", "files", "tests/x.toml")
            },
            new Object[] {
              "praksis_agent_propose",
              Map.of("layer", "m", "rule", "r", "files", List.of(Map.of("path", 1)))
            },
            new Object[] {
              "praksis_agent_propose",
              Map.of("layer", "m", "rule", "r", "files", List.of(Map.of("path", "p", "mode", "x")))
            },
            new Object[] {
              "praksis_agent_propose",
              Map.of("layer", "m", "rule", "r", "files", List.of(Map.of("path", "p")), "x", "y")
            },
            new Object[] {"praksis_plan_propose", Map.of("request", "r", "steps", List.of())},
            new Object[] {
              "praksis_plan_propose",
              Map.of("request", "r", "steps", List.of(Map.of("capability", 5)))
            },
            new Object[] {
              "praksis_plan_propose",
              Map.of("request", "r", "steps", List.of(Map.of("capability", "c", "args", "x")))
            },
            new Object[] {"praksis_notes_save", Map.of("title", "t")},
            new Object[] {"praksis_notes_save", Map.of("title", "t", "text", 3)});
    try (var daemon = new FakeDaemon(200, b -> ANSWER)) {
      for (Object[] c : bad) {
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) c[1];
        assertThatThrownBy(
                () -> tool((String) c[0]).run(new DaemonClient(daemon.base(), "t"), args),
                c[0] + " " + args)
            .isInstanceOf(DaemonException.class);
      }
      assertThat(daemon.seen).isEmpty();
    }
  }

  @Test
  void theDaemonsRefusalOfAReadOnlyTokenReachesTheAgentInItsWords() throws Exception {
    try (var daemon =
        new FakeDaemon(403, b -> "{\"type\":\"error\",\"message\":\"this token is read only\"}")) {
      assertThatThrownBy(
              () ->
                  tool("praksis_notes_save")
                      .run(new DaemonClient(daemon.base(), "t"), Map.of("title", "t", "text", "x")))
          .hasMessage("this token is read only");
    }
  }
}
