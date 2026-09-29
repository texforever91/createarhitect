package dev.createarchitect.network;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import dev.createarchitect.CreateArchitect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCannonSchematicPayload(BlockPos cannonPos, BlockPos anchor,
                                           Rotation rotation, Mirror mirror)
        implements CustomPacketPayload {
    public static final Type<UpdateCannonSchematicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateArchitect.MOD_ID, "update_cannon_schematic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCannonSchematicPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.cannonPos);
                BlockPos.STREAM_CODEC.encode(buffer, payload.anchor);
                buffer.writeEnum(payload.rotation);
                buffer.writeEnum(payload.mirror);
            }, buffer -> new UpdateCannonSchematicPayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    BlockPos.STREAM_CODEC.decode(buffer),
                    buffer.readEnum(Rotation.class),
                    buffer.readEnum(Mirror.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateCannonSchematicPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
            return;
        if (player.distanceToSqr(Vec3Util.center(payload.cannonPos)) > 16 * 16)
            return;
        if (!(player.level().getBlockEntity(payload.cannonPos) instanceof SchematicannonBlockEntity cannon))
            return;
        if (cannon.state != SchematicannonBlockEntity.State.STOPPED)
            return;
        if (!payload.anchor.closerThan(payload.cannonPos, SchematicannonBlockEntity.MAX_ANCHOR_DISTANCE))
            return;

        ItemStack schematic = cannon.inventory.getStackInSlot(0);
        if (!AllItems.SCHEMATIC.isIn(schematic)
                || !schematic.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false))
            return;

        schematic.set(AllDataComponents.SCHEMATIC_ANCHOR, payload.anchor);
        schematic.set(AllDataComponents.SCHEMATIC_ROTATION, payload.rotation);
        schematic.set(AllDataComponents.SCHEMATIC_MIRROR, payload.mirror);
        SchematicInstances.clearHash(schematic);
        cannon.notifyUpdate();
    }

    private static final class Vec3Util {
        private static net.minecraft.world.phys.Vec3 center(BlockPos pos) {
            return net.minecraft.world.phys.Vec3.atCenterOf(pos);
        }
    }
}
