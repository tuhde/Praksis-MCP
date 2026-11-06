// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reading the arguments of a tool call, and writing their schemas. */
final class Args {
  private Args() {}

  static String text(Map<String, Object> args, String name, boolean required)
      throws DaemonException {
    Object v = args.get(name);
    if (v == null) {
      if (required) {
        throw new DaemonException("'" + name + "' is required");
      }
      return null;
    }
    if (!(v instanceof String s) || (required && s.isBlank())) {
      throw new DaemonException("'" + name + "' must be a text");
    }
    return s;
  }

  static int integer(Map<String, Object> args, String name, int fallback, int min, int max)
      throws DaemonException {
    Object v = args.get(name);
    if (v == null) {
      return fallback;
    }
    if (!(v instanceof Number n) || n.doubleValue() != n.longValue()) {
      throw new DaemonException("'" + name + "' must be a whole number");
    }
    long l = n.longValue();
    if (l < min || l > max) {
      throw new DaemonException("'" + name + "' must be between " + min + " and " + max);
    }
    return (int) l;
  }

  static boolean bool(Map<String, Object> args, String name) throws DaemonException {
    Object v = args.get(name);
    if (v == null) {
      return false;
    }
    if (!(v instanceof Boolean b)) {
      throw new DaemonException("'" + name + "' must be true or false");
    }
    return b;
  }

  /**
   * Checks the given arguments against the properties of a schema (type, and range for whole
   * numbers) and refuses names that are not in it. Nothing is sent before this has passed.
   */
  static void validate(Map<String, Object> properties, Map<String, Object> args, String... also)
      throws DaemonException {
    java.util.Set<String> known = new java.util.HashSet<>(properties.keySet());
    known.addAll(java.util.List.of(also));
    for (String key : args.keySet()) {
      if (!known.contains(key)) {
        throw new DaemonException("unknown argument '" + key + "'");
      }
    }
    for (Map.Entry<String, Object> e : properties.entrySet()) {
      if (!(e.getValue() instanceof Map<?, ?> schema)) {
        continue;
      }
      switch (String.valueOf(schema.get("type"))) {
        case "string" -> text(args, e.getKey(), false);
        case "boolean" -> bool(args, e.getKey());
        case "integer" ->
            integer(
                args,
                e.getKey(),
                ((Number) schema.get("minimum")).intValue(),
                ((Number) schema.get("minimum")).intValue(),
                ((Number) schema.get("maximum")).intValue());
        default -> {}
      }
    }
  }

  /** The arguments that were given, without the unset ones, as a map for a capability. */
  static Map<String, Object> only(Map<String, Object> args, String... names) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (String n : names) {
      if (args.get(n) != null) {
        out.put(n, args.get(n));
      }
    }
    return out;
  }

  static Map<String, Object> textSchema(String description) {
    return Map.of("type", "string", "description", description);
  }

  static Map<String, Object> intSchema(String description, int min, int max) {
    return Map.of("type", "integer", "description", description, "minimum", min, "maximum", max);
  }

  static Map<String, Object> boolSchema(String description) {
    return Map.of("type", "boolean", "description", description);
  }
}
