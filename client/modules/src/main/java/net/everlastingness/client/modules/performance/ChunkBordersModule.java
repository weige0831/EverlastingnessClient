package net.everlastingness.client.modules.performance;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Chunk Borders module — shows the chunk boundary grid (F3+G style),
 * mirroring Lunar's ChunkBorders mod.
 */
public class ChunkBordersModule extends AbstractModule {
    @Override public String getId() { return "chunk_borders"; }
    @Override public String getName() { return "Chunk Borders"; }
    @Override public String getDescription() { return "Shows chunk boundaries."; }
    @Override public String getCategory() { return "PERFORMANCE"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
