// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * The settings, from the environment so that the token is not on a command line that others can
 * list: {@code PRAKSIS_TOKEN} (required), {@code PRAKSIS_URL} (default {@code
 * http://127.0.0.1:8484}) and {@code PRAKSIS_MODE} ({@code read-only}, the default, or {@code
 * commands}).
 *
 * @param base the address of the web listener, scheme, host and port only
 * @param token the web token
 * @param mode which tools are offered
 */
public record Config(URI base, String token, Mode mode) {
  @Override
  public String toString() {
    return "Config[base=" + base + ", mode=" + mode + "]";
  }

  /** Reads the settings; a bad one is an {@link IllegalArgumentException} that names it. */
  public static Config from(Map<String, String> env) {
    String token = env.get("PRAKSIS_TOKEN");
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException(
          "PRAKSIS_TOKEN is not set (make one with: praksis web token create NAME --read-only)");
    }
    String mode = env.get("PRAKSIS_MODE");
    return new Config(
        base(env.getOrDefault("PRAKSIS_URL", "http://127.0.0.1:8484")),
        token.strip(),
        Mode.parse(mode == null ? "read-only" : mode.strip()));
  }

  static URI base(String text) {
    URI uri;
    try {
      uri = new URI(text.strip());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(
          "PRAKSIS_URL is not an address like http://127.0.0.1:8484");
    }
    String scheme = uri.getScheme();
    if (!"http".equals(scheme) && !"https".equals(scheme)) {
      throw new IllegalArgumentException("PRAKSIS_URL must start with http:// or https://");
    }
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("PRAKSIS_URL has no host");
    }
    if (uri.getUserInfo() != null) {
      throw new IllegalArgumentException("PRAKSIS_URL must not contain a user name or password");
    }
    try {
      return new URI(scheme, null, uri.getHost(), uri.getPort(), null, null, null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("PRAKSIS_URL is not an address", e);
    }
  }
}
