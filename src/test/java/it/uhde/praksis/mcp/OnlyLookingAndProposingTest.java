// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * What the server can ever ask of the daemon: the questions and the proposals, and nothing that
 * gives, widens or answers a permission. The names of the tools are checked in {@code ToolsTest};
 * this checks what they send.
 */
class OnlyLookingAndProposingTest {
  /** The requests of the protocol that the server may send, and no others. */
  private static final Set<String> ALLOWED =
      Set.of(
          "status",
          "effective",
          "layers",
          "inspect",
          "log",
          "audit",
          "approvals",
          "proposals",
          "proposal-show",
          "agent-plans",
          "agent-plan-show",
          "staged",
          "staged-show",
          "notes",
          "help",
          "verify",
          "stage");

  /** Requests that give, widen or answer a permission, or change the daemon. */
  private static final Set<String> NEVER =
      Set.of(
          "answer",
          "missed",
          "revoke",
          "reload",
          "shadow-promote",
          "shadow-reset",
          "layers-update",
          "proposal-approve",
          "proposal-reject",
          "agent-plan-run",
          "agent-plan-drop",
          "staged-drop",
          "inject",
          "run-command",
          "upload",
          "dictate",
          "entity-create",
          "entity-update",
          "entity-delete",
          "entity-import",
          "pin-set",
          "remote-answer",
          "stop");

  private static final Map<String, Map<String, Object>> SAMPLE =
      Map.ofEntries(
          Map.entry("praksis_rule_read", Map.of("rule", "r")),
          Map.entry("praksis_history_trace", Map.of("event", "e")),
          Map.entry("praksis_proposal_show", Map.of("name", "l@b")),
          Map.entry("praksis_plan_show", Map.of("id", "p")),
          Map.entry("praksis_staged_show", Map.of("id", "s")),
          Map.entry(
              "praksis_agent_propose",
              Map.of(
                  "layer",
                  "l",
                  "rule",
                  "r",
                  "files",
                  List.of(Map.of("path", "tests/r.toml", "content", "x")))),
          Map.entry(
              "praksis_plan_propose",
              Map.of("request", "r", "steps", List.of(Map.of("capability", "c")))),
          Map.entry("praksis_notes_save", Map.of("title", "t", "text", "x")));

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void everyToolAsksOnlyAllowedRequestsAndNeverAForbiddenOne() throws Exception {
    Set<String> sent = new TreeSet<>();
    try (var daemon = new FakeDaemon(200, b -> "{\"type\":\"done\",\"message\":\"ok\"}")) {
      var client = new DaemonClient(daemon.base(), "t");
      for (Tool t : Tools.catalog()) {
        int before = daemon.seen.size();
        t.run(client, SAMPLE.getOrDefault(t.name(), Map.of()));
        // a tool added later must send something, and this test must know its arguments
        assertThat(daemon.seen.size()).as(t.name()).isEqualTo(before + 1);
        JsonNode request = JSON.readTree(daemon.seen.get(before).body());
        String op = request.get("op").asText();
        assertThat(ALLOWED).as(t.name() + " sends " + op).contains(op);
        assertThat(NEVER).as(t.name() + " sends " + op).doesNotContain(op);
        if (op.equals("stage")) {
          assertThat(request.get("what").asText())
              .isIn("agent.propose", "plan.propose", "notes.save");
        }
        if (op.equals("inspect")) {
          assertThat(request.get("what").asText())
              .isIn(
                  "history.find",
                  "history.trace",
                  "history.summary",
                  "rules.list",
                  "rules.read",
                  "plan.catalog");
        }
        if (op.equals("verify")) {
          assertThat(request.get("update").asBoolean()).isFalse();
        }
        sent.add(op);
      }
    }
    // every allowed request is used by some tool, so the list does not grow unnoticed
    assertThat(sent).isEqualTo(ALLOWED);
  }

  @Test
  void theTwoListsDoNotOverlap() {
    assertThat(ALLOWED).doesNotContainAnyElementsOf(NEVER);
  }
}
