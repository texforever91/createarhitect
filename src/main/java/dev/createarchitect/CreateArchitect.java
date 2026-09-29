package dev.createarchitect;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateArchitect.MOD_ID)
public final class CreateArchitect {
    public static final String MOD_ID = "createarchitect";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateArchitect(IEventBus modBus, ModContainer container) {
        LOGGER.info("Create: Architect initialized");
    }
}
