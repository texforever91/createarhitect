package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DownloadSharedPreviewPayload(BlockPos cannonPos, String expectedHash)
        implements CustomPacketPayload {
    public static final Type<DownloadSharedPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "download_shared_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadSharedPreviewPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.cannonPos);
                buffer.writeUtf(payload.expectedHash, 64);
            }, buffer -> new DownloadSharedPreviewPayload(
                    BlockPos.STREAM_CODEC.decode(buffer), buffer.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DownloadSharedPreviewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewServer.sendSchematic(
                context.player(), payload.cannonPos, payload.expectedHash));
    }
}
