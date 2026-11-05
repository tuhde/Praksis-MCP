// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

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

  /** The tools, all modes together. */
  public static List<Tool> catalog() {
    return ReadTools.all();
  }

  /** The tools that a mode offers: read-only offers the tools that look. */
  public static List<Tool> forMode(List<Tool> all, Mode mode) {
    return all.stream().filter(t -> t.kind() == Tool.Kind.READ || mode == Mode.COMMANDS).toList();
  }
}
