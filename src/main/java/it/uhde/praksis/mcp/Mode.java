// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

/**
 * Which tools are offered: {@code read-only} the tools that look, {@code propose} also those that
 * hand the person a proposal (test files, a plan, a note), {@code commands} also those that start
 * commands. The daemon enforces the token either way; this only decides what the client sees.
 */
public enum Mode {
  READ_ONLY("read-only"),
  PROPOSE("propose"),
  COMMANDS("commands");

  private final String text;

  Mode(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }

  static Mode parse(String text) {
    for (Mode m : values()) {
      if (m.text.equals(text)) {
        return m;
      }
    }
    throw new IllegalArgumentException(
        "PRAKSIS_MODE must be \"read-only\", \"propose\" or \"commands\"");
  }
}
