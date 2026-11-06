package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ControlPuppets {
    private record Puppet(LivingEntity Body, ControlLook Look, CompoundTag Identity, Level World, int Revision) {
    }

    private static final Map<UUID, Puppet> Puppets = new HashMap<>();
    private static final VertexConsumer Sink = new VertexConsumer() {
        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int Red, int Green, int Blue, int Alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            return this;
        }
    };
    private static final MultiBufferSource Discard = Type -> Sink;
    @Nullable
    private static Player Capturing;
    private static boolean Captured;

    private ControlPuppets() {
    }

    public static void forget(UUID Player) {
        Puppets.remove(Player);
    }

    public static void clear() {
        Puppets.clear();
    }

    @Nullable
    private static Puppet puppetFor(AbstractClientPlayer User) {
        ControlClientState.View View = ControlClientState.view(User.getUUID());
        if (View == null || !(User.level() instanceof ClientLevel World)) {
            Puppets.remove(User.getUUID());
            return null;
        }
        Puppet Existing = Puppets.get(User.getUUID());
        if (Existing != null && Existing.World() == World && Existing.Identity().equals(View.Identity())) {
            return Existing;
        }
        ControlLook Look = ControlLooks.get(View.Look());
        if (Look == null) {
            return null;
        }
        LivingEntity Body = Look.createPuppet(World, View.Identity());
        if (Body == null) {
            return null;
        }
        Puppet Made = new Puppet(Body, Look, View.Identity(), World, ControlClientState.revision());
        Look.mirror(Body, User, View.Identity());
        Body.xo = User.xo;
        Body.yo = User.yo;
        Body.zo = User.zo;
        Puppets.put(User.getUUID(), Made);
        return Made;
    }

    public static void tick(ClientLevel World) {
        Iterator<Map.Entry<UUID, Puppet>> Each = Puppets.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<UUID, Puppet> Entry = Each.next();
            Player Owner = World.getPlayerByUUID(Entry.getKey());
            if (Owner == null || Entry.getValue().World() != World || !ControlClientState.isControlled(Entry.getKey())) {
                Each.remove();
            }
        }
        for (Player Found : World.players()) {
            if (!(Found instanceof AbstractClientPlayer User) || !ControlClientState.isControlled(User.getUUID())) {
                continue;
            }
            Puppet Current = puppetFor(User);
            if (Current != null) {
                Current.Look().mirror(Current.Body(), User, Current.Identity());
                Current.Look().tickPuppet(Current.Body(), User);
            }
        }
    }

    public static boolean render(Player Rendered, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (!(Rendered instanceof AbstractClientPlayer User) || User.isSpectator()) {
            return false;
        }
        Puppet Current = puppetFor(User);
        if (Current == null) {
            return false;
        }
        Minecraft Game = Minecraft.getInstance();
        if (User.isInvisible() && Game.player != null && User.isInvisibleTo(Game.player)) {
            return true;
        }
        LivingEntity Body = Current.Body();
        Body.setCustomNameVisible(User != Game.getCameraEntity() && !User.isInvisible());
        EntityRenderer<? super LivingEntity> Renderer = Game.getEntityRenderDispatcher().getRenderer(Body);
        float Yaw = Mth.rotLerp(PartialTick, User.yRotO, User.getYRot());
        if (capture(User, Yaw, PartialTick, Pose, Light)) {
            Current.Look().copyPose(Body, User, ((PlayerRenderer) Game.getEntityRenderDispatcher().getRenderer(User)).getModel());
        } else {
            Current.Look().dropPose(Body);
        }
        try {
            Renderer.render(Body, Yaw, PartialTick, Pose, Buffers, Light);
        } finally {
            Current.Look().dropPose(Body);
        }
        return true;
    }

    public static boolean isCapturing(Player Rendered) {
        return Capturing == Rendered;
    }

    public static void captured(Player Rendered) {
        if (Capturing == Rendered) {
            Captured = true;
        }
    }

    private static boolean capture(AbstractClientPlayer User, float Yaw, float PartialTick, PoseStack Pose, int Light) {
        if (Capturing != null || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User) instanceof PlayerRenderer Real)) {
            return false;
        }
        Capturing = User;
        Captured = false;
        Pose.pushPose();
        try {
            Real.render(User, Yaw, PartialTick, Pose, Discard, Light);
        } finally {
            Pose.popPose();
            Capturing = null;
        }
        return Captured;
    }

    public static boolean renderArm(AbstractClientPlayer User, HumanoidArm Arm, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        ControlClientState.View View = ControlClientState.view(User.getUUID());
        if (View == null) {
            return false;
        }
        ControlLook Look = ControlLooks.get(View.Look());
        return Look != null && Look.renderArm(Pose, Buffers, Light, User, Arm, View.Identity());
    }

    public static void mirrorBasics(LivingEntity Body, LivingEntity User, CompoundTag Identity) {
        Body.xo = User.xo;
        Body.yo = User.yo;
        Body.zo = User.zo;
        Body.xOld = User.xOld;
        Body.yOld = User.yOld;
        Body.zOld = User.zOld;
        Body.setPos(User.getX(), User.getY(), User.getZ());
        Body.yRotO = User.yRotO;
        Body.xRotO = User.xRotO;
        Body.setYRot(User.getYRot());
        Body.setXRot(User.getXRot());
        Body.yBodyRotO = User.yBodyRotO;
        Body.yBodyRot = User.yBodyRot;
        Body.yHeadRotO = User.yHeadRotO;
        Body.yHeadRot = User.yHeadRot;
        Body.setOnGround(User.onGround());
        Body.setDeltaMovement(User.getDeltaMovement());
        Body.setPose(User.getPose());
        Body.setShiftKeyDown(User.isShiftKeyDown());
        Body.setSprinting(User.isSprinting());
        Body.setSwimming(User.isSwimming());
        Body.setInvisible(User.isInvisible());
        Body.hurtTime = User.hurtTime;
        Body.hurtDuration = User.hurtDuration;
        Body.deathTime = User.deathTime;
        Body.swinging = User.swinging;
        Body.swingTime = User.swingTime;
        Body.swingingArm = User.swingingArm;
        Body.attackAnim = User.attackAnim;
        Body.oAttackAnim = User.oAttackAnim;
        Body.setRemainingFireTicks(User.getRemainingFireTicks());
        Body.setTicksFrozen(User.getTicksFrozen());
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            if (Slot.getType() != EquipmentSlot.Type.ANIMAL_ARMOR) {
                Body.setItemSlot(Slot, User.getItemBySlot(Slot));
            }
        }
        String Name = Identity.getString("ControlName");
        Body.setCustomName(Name.isEmpty() ? null : Component.literal(Name));
    }
}
