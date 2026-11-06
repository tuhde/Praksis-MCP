// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.uhde.praksis.protocol.Response;
import it.uhde.praksis.protocol.Wire;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReadToolsTest {
  private static final String DONE = "{\"type\":\"done\",\"message\":\"ok\"}";

  private static Tool tool(String name) {
    return Tools.catalog().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
  }

  /** Runs the tool against a fake daemon; returns the request that the daemon saw. */
  private static String sent(String name, Map<String, Object> args) throws Exception {
    try (var daemon = new FakeDaemon(200, b -> DONE)) {
      tool(name).run(new DaemonClient(daemon.base(), "t"), args);
      assertThat(daemon.seen).hasSize(1);
      return daemon.seen.get(0).body();
    }
  }

  @Test
  void everyToolOfTheCatalogLooksOnly() {
    List<Tool> reading = Tools.forMode(Tools.catalog(), Mode.READ_ONLY);
    assertThat(reading).hasSizeGreaterThan(15).allMatch(t -> t.kind() == Tool.Kind.READ);
    assertThat(reading)
        .extracting(Tool::name)
        .contains(
            "praksis_status",
            "praksis_effective",
            "praksis_rule_read",
            "praksis_history_find",
            "praksis_history_trace",
            "praksis_audit",
            "praksis_approvals_list",
            "praksis_proposals_list",
            "praksis_plans_list",
            "praksis_help",
            "praksis_verify")
        .doesNotHaveDuplicates();
  }

  @Test
  void toolsAskTheRightQuestionOfTheProtocol() throws Exception {
    assertThat(sent("praksis_effective", Map.of())).isEqualTo("{\"op\":\"effective\"}");
    assertThat(sent("praksis_layers", Map.of())).isEqualTo("{\"op\":\"layers\"}");
    assertThat(sent("praksis_approvals_list", Map.of())).isEqualTo("{\"op\":\"approvals\"}");
    assertThat(sent("praksis_proposals_list", Map.of())).isEqualTo("{\"op\":\"proposals\"}");
    assertThat(sent("praksis_plans_list", Map.of())).isEqualTo("{\"op\":\"agent-plans\"}");
    assertThat(sent("praksis_staged_list", Map.of())).isEqualTo("{\"op\":\"staged\"}");
    assertThat(sent("praksis_staged_show", Map.of("id", "s1")))
        .isEqualTo("{\"op\":\"staged-show\",\"id\":\"s1\"}");
    assertThat(sent("praksis_notes_list", Map.of("limit", 3)))
        .isEqualTo("{\"op\":\"notes\",\"limit\":3}");
    assertThat(sent("praksis_plan_catalog", Map.of()))
        .isEqualTo("{\"op\":\"inspect\",\"what\":\"plan.catalog\",\"args\":{}}");
    assertThat(sent("praksis_audit", Map.of("limit", 5)))
        .isEqualTo("{\"op\":\"audit\",\"limit\":5}");
    assertThat(sent("praksis_audit", Map.of())).isEqualTo("{\"op\":\"audit\",\"limit\":50}");
    assertThat(sent("praksis_log", Map.of("limit", 7))).isEqualTo("{\"op\":\"log\",\"limit\":7}");
    assertThat(sent("praksis_proposal_show", Map.of("name", "mine@fix")))
        .isEqualTo("{\"op\":\"proposal-show\",\"name\":\"mine@fix\"}");
    assertThat(sent("praksis_plan_show", Map.of("id", "p1")))
        .isEqualTo("{\"op\":\"agent-plan-show\",\"id\":\"p1\"}");
  }

  @Test
  void theQuestionsAboutRulesAndHistoryGoThroughInspectWithOnlyTheGivenArguments()
      throws Exception {
    assertThat(sent("praksis_rule_read", Map.of("rule", "mail.10-spam")))
        .isEqualTo(
            "{\"op\":\"inspect\",\"what\":\"rules.read\",\"args\":{\"rule\":\"mail.10-spam\"}}");
    assertThat(sent("praksis_rules_list", Map.of()))
        .isEqualTo("{\"op\":\"inspect\",\"what\":\"rules.list\",\"args\":{}}");
    assertThat(sent("praksis_history_find", Map.of("text", "invoice", "limit", 3)))
        .contains("\"what\":\"history.find\"")
        .contains("\"text\":\"invoice\"")
        .contains("\"limit\":3")
        .doesNotContain("since_hours");
    assertThat(sent("praksis_history_trace", Map.of("event", "e-1")))
        .contains("\"what\":\"history.trace\"")
        .contains("\"event\":\"e-1\"");
    assertThat(sent("praksis_history_summary", Map.of("since_hours", 24)))
        .contains("\"what\":\"history.summary\"")
        .contains("\"since_hours\":24");
  }

  @Test
  void verifyNeverAsksForAnUpdateAndPassesTheCandidate() throws Exception {
    assertThat(
            sent(
                "praksis_verify",
                Map.of(
                    "replacements", Map.of("mine", "/tmp/branch", "mail", "@fix"), "replay", true)))
        .contains("\"op\":\"verify\"")
        .contains("\"replay\":true")
        .contains("\"update\":false")
        .contains("\"mine\":\"/tmp/branch\"")
        .contains("\"mail\":\"@fix\"");
    assertThat(sent("praksis_verify", Map.of())).contains("\"update\":false");
  }

  @Test
  void badArgumentsAreRefusedBeforeAnythingIsSent() throws Exception {
    List<Object[]> bad =
        List.of(
            new Object[] {"praksis_rule_read", Map.of()},
            new Object[] {"praksis_rule_read", Map.of("rule", 5)},
            new Object[] {"praksis_rule_read", Map.of("rule", "x", "extra", "y")},
            new Object[] {"praksis_audit", Map.of("limit", 0)},
            new Object[] {"praksis_audit", Map.of("limit", 100000)},
            new Object[] {"praksis_audit", Map.of("limit", "many")},
            new Object[] {"praksis_audit", Map.of("limit", 1.5)},
            new Object[] {"praksis_history_find", Map.of("since_hours", -1)},
            new Object[] {"praksis_verify", Map.of("replacements", "mine")},
            new Object[] {"praksis_verify", Map.of("replacements", Map.of("mine", 3))},
            new Object[] {"praksis_verify", Map.of("replay", "yes")});
    try (var daemon = new FakeDaemon(200, b -> DONE)) {
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
  void helpShowsAPageAsMarkdownAndTheListAsLines() throws Exception {
    String page = Wire.encode(new Response.HelpPage("user/rules", "Rules", "# Rules\nhello"));
    String list =
        Wire.encode(
            new Response.HelpTopics(
                List.of(
                    new Response.HelpTopics.Topic("user/rules", "Rules"),
                    new Response.HelpTopics.Topic("user/layers", "Layers"))));
    try (var daemon = new FakeDaemon(200, b -> b.contains("user/rules") ? page : list)) {
      var client = new DaemonClient(daemon.base(), "t");
      assertThat(tool("praksis_help").run(client, Map.of("topic", "user/rules")))
          .isEqualTo("# Rules\nhello");
      assertThat(tool("praksis_help").run(client, Map.of()))
          .isEqualTo("user/rules - Rules\nuser/layers - Layers\n");
    }
  }
}
