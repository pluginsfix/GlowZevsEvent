package pluginsfix.glowzevsevent.config;

import java.util.List;

public record ChestEntryConfig(
        int offsetX,
        int offsetY,
        int offsetZ,
        float offsetYaw,
        String name,
        List<String> hologramLines
) {}
