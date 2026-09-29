package dev.createarchitect.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import dev.createarchitect.CreateArchitect;
import dev.createarchitect.network.UpdateCannonSchematicPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ArchitectHologramSession {
    private static final KeyMapping EDIT = new KeyMapping(
            "key.createarchitect.edit_hologram", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.createarchitect");
    private static final double MAX_DISCOVERY_DISTANCE_SQR = 192 * 192;

    private static BlockPos cannonPos;
    private static ItemStack schematic = ItemStack.EMPTY;
    private static boolean editing;
    private static int scanCooldown;

    private ArchitectHologramSession() {}

    public static ItemStack previewStack() {
        return schematic.isEmpty() ? null : schematic;
    }

    public static boolean isVirtualPreview() {
        return !schematic.isEmpty();
    }

    public static boolean isEditing() {
        return editing;
    }

    public static void sync(SchematicTransformation transformation) {
        if (!editing || cannonPos == null)
            return;
        var settings = transformation.toSettings();
        PacketDistributor.sendToServer(new UpdateCannonSchematicPayload(
                cannonPos, transformation.getAnchor(), settings.getRotation(), settings.getMirror()));
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null)
            return;

        while (EDIT.consumeClick())
            toggleEditing(minecraft);

        if (!editing && --scanCooldown <= 0) {
            discoverNearest(minecraft.level, minecraft.player.blockPosition());
            scanCooldown = 20;
        }
    }

    private static void toggleEditing(Minecraft minecraft) {
        if (editing) {
            editing = false;
            message(minecraft, "message.createarchitect.editing_stopped");
            return;
        }

        if (minecraft.hitResult instanceof BlockHitResult hit
                && minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof SchematicannonBlockEntity cannon) {
            if (cannon.state != SchematicannonBlockEntity.State.STOPPED) {
                message(minecraft, "message.createarchitect.cannon_running");
                return;
            }
            if (!select(cannon)) {
                message(minecraft, "message.createarchitect.no_schematic");
                return;
            }
        }

        if (schematic.isEmpty()) {
            message(minecraft, "message.createarchitect.no_schematic");
            return;
        }
        editing = true;
        message(minecraft, "message.createarchitect.editing_started");
    }

    private static void discoverNearest(ClientLevel level, BlockPos playerPos) {
        SchematicannonBlockEntity nearest = null;
        double nearestDistance = MAX_DISCOVERY_DISTANCE_SQR;
        int radius = Math.min(Minecraft.getInstance().options.getEffectiveRenderDistance(), 12);
        int centerX = playerPos.getX() >> 4;
        int centerZ = playerPos.getZ() >> 4;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null)
                    continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof SchematicannonBlockEntity candidate))
                        continue;
                    ItemStack candidateStack = candidate.inventory.getStackInSlot(0);
                    if (!previewable(candidateStack))
                        continue;
                    double distance = candidate.getBlockPos().distSqr(playerPos);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = candidate;
                    }
                }
            }
        }

        if (nearest == null) {
            if (!editing)
                clear();
        } else {
            select(nearest);
        }
    }

    private static boolean select(SchematicannonBlockEntity cannon) {
        ItemStack stack = cannon.inventory.getStackInSlot(0);
        if (!previewable(stack))
            return false;
        cannonPos = cannon.getBlockPos().immutable();
        schematic = stack.copy();
        return true;
    }

    private static boolean previewable(ItemStack stack) {
        return !stack.isEmpty() && AllItems.SCHEMATIC.isIn(stack)
                && stack.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)
                && stack.has(AllDataComponents.SCHEMATIC_ANCHOR);
    }

    private static void message(Minecraft minecraft, String key) {
        minecraft.player.displayClientMessage(Component.translatable(key), true);
    }

    private static void clear() {
        cannonPos = null;
        schematic = ItemStack.EMPTY;
        editing = false;
    }

    @EventBusSubscriber(modid = CreateArchitect.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(EDIT);
        }
    }

    @EventBusSubscriber(modid = CreateArchitect.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            tick();
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }
    }
}
