// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

/** The daemon refused, or could not be reached. The message is safe to show to the agent. */
public class DaemonException extends Exception {
  private static final long serialVersionUID = 1L;

  public DaemonException(String message) {
    super(message);
  }
}
