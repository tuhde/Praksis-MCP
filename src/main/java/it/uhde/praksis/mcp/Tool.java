// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import java.util.List;
import java.util.Map;

/**
 * One tool: a name, a description for the agent, what it does to the daemon, the arguments it takes
 * as a JSON schema, and the code that asks the daemon.
 */
public interface Tool {
  /**
   * What a tool does to the daemon: {@code READ} only looks, {@code PROPOSE} hands the person
   * something to look at (nothing is applied, run or granted), {@code COMMAND} starts something.
   */
  enum Kind {
    READ,
    PROPOSE,
    COMMAND
  }

  /** A body that asks the daemon. */
  @FunctionalInterface
  interface Body {
    String run(DaemonClient daemon, Map<String, Object> args) throws DaemonException;
  }

  String name();

  String title();

  String description();

  Kind kind();

  /** The schema of the arguments (type "object"). */
  Map<String, Object> input();

  String run(DaemonClient daemon, Map<String, Object> args) throws DaemonException;

  /** A tool without arguments. */
  static Tool of(String name, String title, String description, Kind kind, Body body) {
    return of(name, title, description, kind, Map.of(), List.of(), body);
  }

  /** A tool with the given properties (JSON schema fragments) and required names. */
  static Tool of(
      String name,
      String title,
      String description,
      Kind kind,
      Map<String, Object> properties,
      List<String> required,
      Body body) {
    Map<String, Object> schema =
        Map.of(
            "type",
            "object",
            "properties",
            properties,
            "required",
            required,
            "additionalProperties",
            false);
    return new Tool() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String title() {
        return title;
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public Kind kind() {
        return kind;
      }

      @Override
      public Map<String, Object> input() {
        return schema;
      }

      @Override
      public String run(DaemonClient daemon, Map<String, Object> args) throws DaemonException {
        return body.run(daemon, args);
      }
    };
  }
}
