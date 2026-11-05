// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import it.uhde.praksis.protocol.Request;
import it.uhde.praksis.protocol.Response;
import java.util.List;

/** The tools of the server, and which of them a mode offers. */
public final class Tools {
  private Tools() {}

  /**
   * Names that must never be part of a tool name: they give, widen or answer a permission. A test
   * checks the catalog against this list.
   */
  public static final List<String> FORBIDDEN =
      List.of(
          "approve",
          "answer",
          "grant",
          "revoke",
          "pin",
          "trust",
          "plan_run",
          "plan_drop",
          "layers_update",
          "reveal",
          "entity_create",
          "entity_update",
          "entity_delete",
          "reload",
          "stop",
          "inject");

  /** The tools, all modes together; the read tools follow in the next task. */
  public static List<Tool> catalog() {
    return List.of(status());
  }

  /** The tools that a mode offers: read-only offers the tools that look. */
  public static List<Tool> forMode(List<Tool> all, Mode mode) {
    return all.stream().filter(t -> t.kind() == Tool.Kind.READ || mode == Mode.COMMANDS).toList();
  }

  private static Tool status() {
    return Tool.of(
        "praksis_status",
        "Status of the Praksis daemon",
        "The state of the daemon: its ruleset, the number of rules, the modules, how many events"
            + " wait, the protocol version and the identity it serves.",
        Tool.Kind.READ,
        (daemon, args) -> {
          Response reply = daemon.call(new Request.Status());
          return DaemonClient.json(reply);
        });
  }
}
