package pluginsfix.glowzevsevent.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StageConfig(
        int stageNumber,
        String bossbar,
        int durationSeconds,
        int lightningInterval,
        int lightningRadius,
        int skyDropInterval,
        int skyDropRadius,
        Set<String> lightningDestroyingBlocks,
        Map<String, String> flags,
        List<String> chestDisguiseItems,
        long chestTakeCooldownMillis,
        Map<Integer, ChestEntryConfig> chests
) {}
