package dev.mineralwalker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MineralWalker implements ModInitializer {
    public static final String MOD_ID = "mineralwalker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MineralArmorItems.initialize();
        ServerTickEvents.END_LEVEL_TICK.register(MineralTrail::onLevelTick);
        LOGGER.info("Mineral Walker loaded: {} mineral chest armors registered.", MineralArmorItems.itemCount());
    }
}
