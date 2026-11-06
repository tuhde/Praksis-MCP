// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigTest {
  @Test
  void needsATokenAndDefaultsToReadOnlyOnLoopback() {
    assertThatThrownBy(() -> Config.from(Map.of())).hasMessageContaining("PRAKSIS_TOKEN");
    assertThatThrownBy(() -> Config.from(Map.of("PRAKSIS_TOKEN", "  ")))
        .hasMessageContaining("PRAKSIS_TOKEN");
    Config c = Config.from(Map.of("PRAKSIS_TOKEN", " t "));
    assertThat(c.token()).isEqualTo("t");
    assertThat(c.base()).isEqualTo(URI.create("http://127.0.0.1:8484"));
    assertThat(c.mode()).isEqualTo(Mode.READ_ONLY);
  }

  @Test
  void readsTheMode() {
    assertThat(Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_MODE", "commands")).mode())
        .isEqualTo(Mode.COMMANDS);
    assertThat(Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_MODE", "propose")).mode())
        .isEqualTo(Mode.PROPOSE);
    assertThatThrownBy(() -> Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_MODE", "all")))
        .hasMessageContaining("PRAKSIS_MODE");
  }

  @Test
  void checksTheAddress() {
    assertThatThrownBy(() -> Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_URL", "ftp://x")))
        .hasMessageContaining("http");
    assertThatThrownBy(
            () -> Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_URL", "http://u:p@x")))
        .hasMessageContaining("user name");
    assertThatThrownBy(() -> Config.from(Map.of("PRAKSIS_TOKEN", "t", "PRAKSIS_URL", "nonsense")))
        .isInstanceOf(IllegalArgumentException.class);
    // only scheme, host and port are kept
    assertThat(Config.base("http://localhost:9000/some/path?x=1"))
        .isEqualTo(URI.create("http://localhost:9000"));
  }

  @Test
  void theTokenIsNotInTheTextOfTheSettings() {
    assertThat(Config.from(Map.of("PRAKSIS_TOKEN", "pwt_secret")).toString())
        .doesNotContain("pwt_secret");
  }
}
