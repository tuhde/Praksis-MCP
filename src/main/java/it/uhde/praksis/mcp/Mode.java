// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

/** Whether the tools that start commands are offered. The daemon enforces the token either way. */
public enum Mode {
  READ_ONLY("read-only"),
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
    throw new IllegalArgumentException("PRAKSIS_MODE must be \"read-only\" or \"commands\"");
  }
}
