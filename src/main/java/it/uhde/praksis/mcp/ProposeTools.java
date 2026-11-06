// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import it.uhde.praksis.protocol.Request;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tools that hand the person something to look at. Nothing they make is applied, run or
 * granted: test files are staged, a plan waits for the person to run it, a note waits to be read.
 * The daemon checks all of it again and audits each use under the name of the token.
 */
final class ProposeTools {
  private ProposeTools() {}

  private static final Tool.Kind PROPOSE = Tool.Kind.PROPOSE;

  static List<Tool> all() {
    return List.of(agentPropose(), planPropose(), notesSave());
  }

  private static Map<String, Object> props(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      m.put((String) pairs[i], pairs[i + 1]);
    }
    return m;
  }

  private static Map<String, Object> listOf(String description, Map<String, Object> item) {
    return Map.of("type", "array", "description", description, "items", item);
  }

  /** Checks that a value is a list of objects whose given keys are texts (or objects). */
  private static void objects(
      Map<String, Object> args, String name, int max, Map<String, Class<?>> keys)
      throws DaemonException {
    Object v = args.get(name);
    if (!(v instanceof List<?> list) || list.isEmpty() || list.size() > max) {
      throw new DaemonException("'" + name + "' needs 1 to " + max + " entries");
    }
    for (Object o : list) {
      if (!(o instanceof Map<?, ?> m)) {
        throw new DaemonException("every entry of '" + name + "' must be an object");
      }
      for (Map.Entry<String, Class<?>> k : keys.entrySet()) {
        Object value = m.get(k.getKey());
        if (value != null && !k.getValue().isInstance(value)) {
          throw new DaemonException(
              "'" + k.getKey() + "' in '" + name + "' must be " + k.getValue().getSimpleName());
        }
      }
      for (Object key : m.keySet()) {
        if (!keys.containsKey(key)) {
          throw new DaemonException("unknown key '" + key + "' in '" + name + "'");
        }
      }
    }
  }

  private static String stage(DaemonClient d, String what, Map<String, Object> args)
      throws DaemonException {
    return DaemonClient.json(d.call(new Request.Stage(what, args, null)));
  }

  private static Tool agentPropose() {
    Map<String, Object> properties =
        props(
            "layer", Args.textSchema("the layer that owns the rule; it must take proposals"),
            "rule", Args.textSchema("the rule id the tests are for"),
            "files",
                listOf(
                    "the test files to add: {path: tests/<name>.toml, content}",
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        props(
                            "path", Args.textSchema("tests/<name>.toml"),
                            "content", Args.textSchema("the test, in TOML")),
                        "required",
                        List.of("path", "content"),
                        "additionalProperties",
                        false)),
            "reason", Args.textSchema("why, for the person who reads it (at most 500 characters)"));
    return Tool.of(
        "praksis_agent_propose",
        "Propose tests for a rule",
        "Stages test files for a rule that has none. The daemon runs the checks of the layer with"
            + " them and stages them only if they cover the rule and add no problem. A proposal"
            + " only adds files named tests/<name>.toml; nothing is changed or applied. The person"
            + " looks at it with `praksis agent show` or in the Agent tab and exports or drops it."
            + " Read the rule first (praksis_rule_read) and the page on tests (praksis_help).",
        PROPOSE,
        properties,
        List.of("layer", "rule", "files"),
        (d, a) -> {
          Args.validate(onlyTextProps(properties), a, "files");
          Args.text(a, "layer", true);
          Args.text(a, "rule", true);
          objects(a, "files", 10, Map.of("path", String.class, "content", String.class));
          return stage(d, "agent.propose", Args.only(a, "layer", "rule", "files", "reason"));
        });
  }

  /** The properties that are plain texts, for the check of the others by hand. */
  private static Map<String, Object> onlyTextProps(Map<String, Object> properties) {
    Map<String, Object> out = new LinkedHashMap<>();
    properties.forEach(
        (k, v) -> {
          if (v instanceof Map<?, ?> m && "string".equals(m.get("type"))) {
            out.put(k, v);
          }
        });
    return out;
  }

  private static Tool planPropose() {
    Map<String, Object> properties =
        props(
            "request", Args.textSchema("what the person asked for, in their words"),
            "steps",
                listOf(
                    "the steps in order: {capability, args}; only capabilities from"
                        + " praksis_plan_catalog",
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        props(
                            "capability", Args.textSchema("for example mail.move"),
                            "args", Map.of("type", "object")),
                        "required",
                        List.of("capability"),
                        "additionalProperties",
                        false)),
            "context",
                Args.textSchema(
                    "what you read to make the plan, as it was; a value in a step that is neither"
                        + " in the request nor here is marked for the person"),
            "reason", Args.textSchema("why this plan (at most 500 characters)"));
    return Tool.of(
        "praksis_plan_propose",
        "Propose a plan",
        "Keeps a plan that the person can read and run. It runs nothing: each step is checked"
            + " against the grants of the plan layer as they are now, and the person decides. Look"
            + " at praksis_plan_catalog first. Values in steps that are not in the request or in"
            + " the context are marked for the person.",
        PROPOSE,
        properties,
        List.of("request", "steps"),
        (d, a) -> {
          Args.validate(onlyTextProps(properties), a, "steps");
          Args.text(a, "request", true);
          objects(a, "steps", 200, Map.of("capability", String.class, "args", Map.class));
          return stage(d, "plan.propose", Args.only(a, "request", "steps", "context", "reason"));
        });
  }

  private static Tool notesSave() {
    Map<String, Object> properties =
        props(
            "title", Args.textSchema("one line, at most 120 characters"),
            "text", Args.textSchema("the note, at most 64 KiB"));
    return Tool.of(
        "praksis_notes_save",
        "Leave a note for the person",
        "Saves a note (an explanation, an answer) that the person reads in the Agent tab of the"
            + " desktop program. The newest 200 are kept.",
        PROPOSE,
        properties,
        List.of("title", "text"),
        (d, a) -> {
          Args.validate(properties, a);
          Args.text(a, "title", true);
          Args.text(a, "text", true);
          return stage(d, "notes.save", Args.only(a, "title", "text"));
        });
  }
}
