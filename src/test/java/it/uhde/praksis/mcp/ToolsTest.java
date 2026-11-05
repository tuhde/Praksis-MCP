// SPDX-License-Identifier: Apache-2.0
package it.uhde.praksis.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolsTest {
  static Tool writer() {
    return Tool.of(
        "praksis_run_command",
        "Start a command",
        "test only",
        Tool.Kind.COMMAND,
        (daemon, args) -> "started");
  }

  static List<Tool> withWriter() {
    List<Tool> all = new ArrayList<>(Tools.catalog());
    all.add(writer());
    return all;
  }

  @Test
  void readOnlyOffersOnlyToolsThatLook() {
    assertThat(Tools.forMode(withWriter(), Mode.READ_ONLY))
        .extracting(Tool::name)
        .contains("praksis_status")
        .doesNotContain("praksis_run_command");
    assertThat(Tools.forMode(withWriter(), Mode.COMMANDS))
        .extracting(Tool::name)
        .contains("praksis_status", "praksis_run_command");
  }

  @Test
  void noToolIsNamedLikeSomethingThatGivesOrAnswersAPermission() {
    for (Tool t : withWriter()) {
      for (String bad : Tools.FORBIDDEN) {
        assertThat(t.name()).as(t.name()).doesNotContain(bad);
      }
    }
  }

  @Test
  void everyToolNamesItselfPraksisAndTakesNoUndeclaredArguments() {
    for (Tool t : withWriter()) {
      assertThat(t.name()).startsWith("praksis_");
      assertThat(t.input()).containsEntry("additionalProperties", false);
    }
  }
}
