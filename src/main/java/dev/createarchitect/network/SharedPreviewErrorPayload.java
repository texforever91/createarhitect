package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import dev.createarchitect.client.SharedPreviewClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SharedPreviewErrorPayload(String message) implements CustomPacketPayload {
    public static final Type<SharedPreviewErrorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "shared_preview_error"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPreviewErrorPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeUtf(payload.message, 256),
                    buffer -> new SharedPreviewErrorPayload(buffer.readUtf(256)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedPreviewErrorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewClient.receiveError(payload.message));
    }
}
