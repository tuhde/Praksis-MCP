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
    List<Tool> all = new java.util.ArrayList<>(ReadTools.all());
    all.addAll(ProposeTools.all());
    return List.copyOf(all);
  }

  /** The tools that a mode offers; each mode offers those of the modes before it. */
  public static List<Tool> forMode(List<Tool> all, Mode mode) {
    return all.stream().filter(t -> offered(t.kind(), mode)).toList();
  }

  static boolean offered(Tool.Kind kind, Mode mode) {
    return switch (kind) {
      case READ -> true;
      case PROPOSE -> mode == Mode.PROPOSE || mode == Mode.COMMANDS;
      case COMMAND -> mode == Mode.COMMANDS;
    };
  }
}
