package dev.createarchitect.network;

import dev.createarchitect.CreateArchitect;
import dev.createarchitect.client.SharedPreviewClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SharedPreviewStartPayload(UUID transferId, String owner, String fileName,
                                        String hash, int compressedBytes, int chunks)
        implements CustomPacketPayload {
    public static final Type<SharedPreviewStartPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "shared_preview_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPreviewStartPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeUUID(payload.transferId);
                buffer.writeUtf(payload.owner, 128);
                buffer.writeUtf(payload.fileName, 512);
                buffer.writeUtf(payload.hash, 64);
                buffer.writeInt(payload.compressedBytes);
                buffer.writeInt(payload.chunks);
            }, buffer -> new SharedPreviewStartPayload(buffer.readUUID(), buffer.readUtf(128),
                    buffer.readUtf(512), buffer.readUtf(64), buffer.readInt(), buffer.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedPreviewStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedPreviewClient.startTransfer(payload));
    }
}
