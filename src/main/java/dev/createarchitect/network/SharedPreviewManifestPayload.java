package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import dev.createarchitect.client.SharedPreviewClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SharedPreviewManifestPayload(BlockPos cannonPos, String owner, String fileName,
                                           long compressedBytes, String hash)
        implements CustomPacketPayload {
    public static final Type<SharedPreviewManifestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "shared_preview_manifest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPreviewManifestPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.cannonPos);
                buffer.writeUtf(payload.owner, 128);
                buffer.writeUtf(payload.fileName, 512);
                buffer.writeLong(payload.compressedBytes);
                buffer.writeUtf(payload.hash, 64);
            }, buffer -> new SharedPreviewManifestPayload(
                    BlockPos.STREAM_CODEC.decode(buffer), buffer.readUtf(128), buffer.readUtf(512),
                    buffer.readLong(), buffer.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedPreviewManifestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewClient.receiveManifest(payload));
    }
}
