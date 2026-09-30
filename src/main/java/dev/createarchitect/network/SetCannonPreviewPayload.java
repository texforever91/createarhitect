package dev.createarchitect.network;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import dev.createarchitect.CreateArchitect;
import dev.createarchitect.SchematicannonPreviewState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetCannonPreviewPayload(BlockPos cannonPos, boolean enabled)
        implements CustomPacketPayload {
    public static final Type<SetCannonPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "set_cannon_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCannonPreviewPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.cannonPos);
                buffer.writeBoolean(payload.enabled);
            }, buffer -> new SetCannonPreviewPayload(
                    BlockPos.STREAM_CODEC.decode(buffer), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetCannonPreviewPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
            return;
        if (player.distanceToSqr(Vec3.atCenterOf(payload.cannonPos)) > 16 * 16)
            return;
        if (!(player.level().getBlockEntity(payload.cannonPos) instanceof SchematicannonBlockEntity cannon))
            return;

        ((SchematicannonPreviewState) cannon).createarchitect$setPreviewEnabled(payload.enabled);
        cannon.setChanged();
        cannon.notifyUpdate();
    }
}
