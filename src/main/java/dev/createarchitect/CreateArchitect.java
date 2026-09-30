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
        var registrar = event.registrar("1");
        registrar.playToServer(
                dev.createarchitect.network.UpdateCannonSchematicPayload.TYPE,
                dev.createarchitect.network.UpdateCannonSchematicPayload.STREAM_CODEC,
                dev.createarchitect.network.UpdateCannonSchematicPayload::handle);
        registrar.playToServer(
                dev.createarchitect.network.SetCannonPreviewPayload.TYPE,
                dev.createarchitect.network.SetCannonPreviewPayload.STREAM_CODEC,
                dev.createarchitect.network.SetCannonPreviewPayload::handle);
        registrar.playToServer(
                dev.createarchitect.network.RequestSharedPreviewManifestPayload.TYPE,
                dev.createarchitect.network.RequestSharedPreviewManifestPayload.STREAM_CODEC,
                dev.createarchitect.network.RequestSharedPreviewManifestPayload::handle);
        registrar.playToServer(
                dev.createarchitect.network.DownloadSharedPreviewPayload.TYPE,
                dev.createarchitect.network.DownloadSharedPreviewPayload.STREAM_CODEC,
                dev.createarchitect.network.DownloadSharedPreviewPayload::handle);
        registrar.playToClient(
                dev.createarchitect.network.SharedPreviewManifestPayload.TYPE,
                dev.createarchitect.network.SharedPreviewManifestPayload.STREAM_CODEC,
                dev.createarchitect.network.SharedPreviewManifestPayload::handle);
        registrar.playToClient(
                dev.createarchitect.network.SharedPreviewStartPayload.TYPE,
                dev.createarchitect.network.SharedPreviewStartPayload.STREAM_CODEC,
                dev.createarchitect.network.SharedPreviewStartPayload::handle);
        registrar.playToClient(
                dev.createarchitect.network.SharedPreviewChunkPayload.TYPE,
                dev.createarchitect.network.SharedPreviewChunkPayload.STREAM_CODEC,
                dev.createarchitect.network.SharedPreviewChunkPayload::handle);
        registrar.playToClient(
                dev.createarchitect.network.SharedPreviewErrorPayload.TYPE,
                dev.createarchitect.network.SharedPreviewErrorPayload.STREAM_CODEC,
                dev.createarchitect.network.SharedPreviewErrorPayload::handle);
    }
}
