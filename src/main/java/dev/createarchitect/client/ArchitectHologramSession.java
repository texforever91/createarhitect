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
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ArchitectHologramSession {
    private static final KeyMapping EDIT = new KeyMapping(
            "key.createarchitect.edit_hologram", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.createarchitect");
    private static final KeyMapping EXIT_FREECAM = new KeyMapping(
            "key.createarchitect.exit_freecam", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.createarchitect");
    private static final double MAX_DISCOVERY_DISTANCE_SQR = 192 * 192;
    private static final double CAMERA_SPEED = 0.55;
    private static final double CAMERA_FAST_SPEED = 1.65;

    private static BlockPos cannonPos;
    private static ItemStack schematic = ItemStack.EMPTY;
    private static boolean editing;
    private static Marker camera;
    private static CameraType previousCameraType;
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

    public static Entity camera() {
        return camera;
    }

    public static boolean turnCamera(double yaw, double pitch) {
        if (camera == null)
            return false;
        camera.turn(yaw, pitch);
        return true;
    }

    public static void openFreecam(SchematicannonBlockEntity cannon) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null)
            return;
        if (!select(cannon)) {
            message(minecraft, "message.createarchitect.no_schematic");
            return;
        }
        minecraft.player.closeContainer();
        startCamera(minecraft);
        message(minecraft, "message.createarchitect.freecam_started");
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

        while (EXIT_FREECAM.consumeClick()) {
            if (camera != null) {
                stopCamera(minecraft);
                message(minecraft, "message.createarchitect.freecam_stopped");
            }
        }

        if (camera != null)
            tickCamera(minecraft);

        if (!editing && camera == null && --scanCooldown <= 0) {
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
        if (cannonPos == null
                || !(minecraft.level.getBlockEntity(cannonPos) instanceof SchematicannonBlockEntity selectedCannon)) {
            message(minecraft, "message.createarchitect.no_schematic");
            return;
        }
        if (selectedCannon.state != SchematicannonBlockEntity.State.STOPPED) {
            message(minecraft, "message.createarchitect.cannon_running");
            return;
        }
        editing = true;
        message(minecraft, "message.createarchitect.editing_started");
    }

    private static void startCamera(Minecraft minecraft) {
        if (camera != null || minecraft.player == null || minecraft.level == null)
            return;
        camera = new Marker(EntityType.MARKER, minecraft.level);
        camera.setPos(minecraft.player.getEyePosition());
        camera.setYRot(minecraft.player.getYRot());
        camera.setXRot(minecraft.player.getXRot());
        camera.setOldPosAndRot();
        previousCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.setCameraEntity(camera);
    }

    private static void tickCamera(Minecraft minecraft) {
        if (camera == null || minecraft.getCameraEntity() != camera) {
            stopCamera(minecraft);
            return;
        }
        camera.setOldPosAndRot();
        if (minecraft.screen != null)
            return;

        Vec3 forward = camera.getLookAngle();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() > 0)
            right = right.normalize();
        Vec3 movement = Vec3.ZERO;
        if (minecraft.options.keyUp.isDown()) movement = movement.add(forward);
        if (minecraft.options.keyDown.isDown()) movement = movement.subtract(forward);
        if (minecraft.options.keyRight.isDown()) movement = movement.add(right);
        if (minecraft.options.keyLeft.isDown()) movement = movement.subtract(right);
        if (minecraft.options.keyJump.isDown()) movement = movement.add(0, 1, 0);
        if (minecraft.options.keyShift.isDown()) movement = movement.add(0, -1, 0);
        if (movement.lengthSqr() == 0)
            return;

        double speed = minecraft.options.keySprint.isDown() ? CAMERA_FAST_SPEED : CAMERA_SPEED;
        Vec3 next = camera.position().add(movement.normalize().scale(speed));
        if (cannonPos != null) {
            Vec3 center = Vec3.atCenterOf(cannonPos);
            Vec3 offset = next.subtract(center);
            double maxDistance = Math.sqrt(MAX_DISCOVERY_DISTANCE_SQR);
            if (offset.lengthSqr() > MAX_DISCOVERY_DISTANCE_SQR)
                next = center.add(offset.normalize().scale(maxDistance));
        }
        camera.setPos(next);
    }

    private static void stopCamera(Minecraft minecraft) {
        if (camera == null)
            return;
        if (minecraft.getCameraEntity() == camera)
            minecraft.setCameraEntity(minecraft.player);
        if (previousCameraType != null)
            minecraft.options.setCameraType(previousCameraType);
        camera = null;
        previousCameraType = null;
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
        stopCamera(Minecraft.getInstance());
        cannonPos = null;
        schematic = ItemStack.EMPTY;
        editing = false;
    }

    @EventBusSubscriber(modid = CreateArchitect.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(EDIT);
            event.register(EXIT_FREECAM);
        }
    }

    @EventBusSubscriber(modid = CreateArchitect.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            tick();
        }

        @SubscribeEvent
        public static void movementInput(MovementInputUpdateEvent event) {
            if (camera == null)
                return;
            Input input = event.getInput();
            input.leftImpulse = 0;
            input.forwardImpulse = 0;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }
    }
}
