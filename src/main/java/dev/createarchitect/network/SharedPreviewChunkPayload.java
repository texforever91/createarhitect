package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import dev.createarchitect.client.SharedPreviewClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SharedPreviewChunkPayload(UUID transferId, int index, byte[] data)
        implements CustomPacketPayload {
    public static final int MAX_CHUNK_BYTES = 24 * 1024;
    public static final Type<SharedPreviewChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "shared_preview_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPreviewChunkPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeUUID(payload.transferId);
                buffer.writeInt(payload.index);
                buffer.writeByteArray(payload.data);
            }, buffer -> new SharedPreviewChunkPayload(
                    buffer.readUUID(), buffer.readInt(), buffer.readByteArray(MAX_CHUNK_BYTES)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedPreviewChunkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewClient.receiveChunk(payload));
    }
}
