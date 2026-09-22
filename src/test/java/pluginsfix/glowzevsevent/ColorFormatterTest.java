package pluginsfix.glowzevsevent;

import org.junit.jupiter.api.Test;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class ColorFormatterTest {

    @Test
    void shouldFormatHexColors() {
        String input = "&#FB9C08Гнев зевса";
        String colorized = ColorFormatter.colorize(input);
        assertThat(colorized.toLowerCase()).contains("§x§f§b§9§c§0§8");
    }

    @Test
    void shouldHandleEmptyAndNull() {
        assertThat(ColorFormatter.colorize("")).isEmpty();
        assertThat(ColorFormatter.colorize(null)).isEmpty();
    }
}
