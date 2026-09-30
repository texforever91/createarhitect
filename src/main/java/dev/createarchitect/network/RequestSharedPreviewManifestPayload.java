package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestSharedPreviewManifestPayload(BlockPos cannonPos) implements CustomPacketPayload {
    public static final Type<RequestSharedPreviewManifestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "request_shared_preview_manifest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSharedPreviewManifestPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> BlockPos.STREAM_CODEC.encode(buffer, payload.cannonPos),
                    buffer -> new RequestSharedPreviewManifestPayload(BlockPos.STREAM_CODEC.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSharedPreviewManifestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewServer.sendManifest(context.player(), payload.cannonPos));
    }
}
