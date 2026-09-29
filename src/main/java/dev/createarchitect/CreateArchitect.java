package dev.createarchitect;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CreateArchitect.MOD_ID)
public final class CreateArchitect {
    public static final String MOD_ID = "createarchitect";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateArchitect(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::registerPayloads);
        LOGGER.info("Create: Architect initialized");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                dev.createarchitect.network.UpdateCannonSchematicPayload.TYPE,
                dev.createarchitect.network.UpdateCannonSchematicPayload.STREAM_CODEC,
                dev.createarchitect.network.UpdateCannonSchematicPayload::handle);
    }
}
