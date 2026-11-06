package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Scp049Mimicry {
    public static final float Lean = 7.0f;
    public static final float Sway = 2.5f;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<Scp049Cured, Mimic> Mimics = new WeakHashMap<>();
    private static final Set<Object> Hooked = Collections.newSetFromMap(new WeakHashMap<>());
    @Nullable
    private static Entity Current;
    private static boolean Angry;

    private static final class Mimic {
        private final LivingEntity Body;
        private final String Type;
        private final int Hash;
        private int Tick = Integer.MIN_VALUE;
        private boolean Ticks = true;
        private boolean Broken;

        private Mimic(LivingEntity Body, String Type, int Hash) {
            this.Body = Body;
            this.Type = Type;
            this.Hash = Hash;
        }
    }

    private Scp049Mimicry() {
    }

    public static boolean isShambling(Entity Body) {
        return Current != null && Current == Body;
    }

    public static boolean angry() {
        return Angry;
    }

    public static boolean render(Scp049Cured Risen, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Mimic Found = mimicFor(Risen);
        if (Found == null || Found.Broken) {
            return false;
        }
        advance(Found, Risen);
        EntityRenderer<? super LivingEntity> Renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Found.Body);
        hook(Renderer);
        Pose.pushPose();
        PoseStack.Pose Mine = Pose.last();
        shamble(Pose, Risen, Partial);
        Current = Found.Body;
        Angry = Risen.isAggressive();
        try {
            Renderer.render(Found.Body, Yaw, Partial, Pose, Buffers, Light);
            return true;
        } catch (RuntimeException Problem) {
            Found.Broken = true;
            Log.warn("SCP-049-2 could not wear the look of {}", Found.Type, Problem);
            return false;
        } finally {
            Current = null;
            Angry = false;
            if (unwind(Pose, Mine)) {
                Pose.popPose();
            }
        }
    }

    public static boolean unwind(PoseStack Pose, PoseStack.Pose Mine) {
        while (Pose.last() != Mine && !Pose.clear()) {
            Pose.popPose();
        }
        return Pose.last() == Mine;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hook(EntityRenderer<?> Renderer) {
        if (Renderer instanceof GeoEntityRenderer Geo && Hooked.add(Geo)) {
            Geo.addRenderLayer(new Scp049ShambleLayer(Geo));
        }
    }

    public static void shamble(PoseStack Pose, Scp049Cured Risen, float Partial) {
        float Body = Mth.rotLerp(Partial, Risen.yBodyRotO, Risen.yBodyRot) * Mth.DEG_TO_RAD;
        float Age = Risen.tickCount + Partial;
        float Cos = Mth.cos(Body);
        float Sin = Mth.sin(Body);
        float Step = Risen.walkAnimation.position(Partial);
        float Moving = Math.min(1.0f, Risen.walkAnimation.speed(Partial) * 1.5f);
        Pose.mulPose(Axis.of(new Vector3f(Cos, 0.0f, Sin)).rotationDegrees(Lean));
        Pose.mulPose(Axis.of(new Vector3f(-Sin, 0.0f, Cos)).rotationDegrees(Mth.sin(Age * 0.05f) * Sway * 0.6f + Mth.sin(Step * 0.6662f) * Sway * Moving));
    }

    @Nullable
    private static Mimic mimicFor(Scp049Cured Risen) {
        String Type = Risen.mimicType();
        CompoundTag Data = Risen.mimicTag();
        if (Type.isEmpty()) {
            return null;
        }
        int Hash = Data.hashCode();
        Mimic Known = Mimics.get(Risen);
        if (Known != null && Known.Type.equals(Type) && Known.Hash == Hash) {
            return Known;
        }
        EntityType<?> Kind = EntityType.byString(Type).orElse(null);
        if (Kind == null) {
            return null;
        }
        Mimic Made = null;
        try {
            Entity Created = Kind.create(Risen.level());
            if (Created instanceof LivingEntity Body) {
                Body.load(Data.copy());
                Body.setSilent(true);
                Body.noPhysics = true;
                Body.setInvisible(false);
                Body.setCustomNameVisible(false);
                Body.setHealth(Body.getMaxHealth());
                Made = new Mimic(Body, Type, Hash);
            }
        } catch (RuntimeException Problem) {
            Log.warn("SCP-049-2 could not rebuild the look of {}", Type, Problem);
        }
        if (Made == null) {
            return null;
        }
        Mimics.put(Risen, Made);
        return Made;
    }

    private static void advance(Mimic Found, Scp049Cured Risen) {
        if (Found.Tick == Risen.tickCount) {
            return;
        }
        LivingEntity Body = Found.Body;
        Body.setOldPosAndRot();
        place(Body, Risen);
        Body.tickCount = Risen.tickCount;
        Body.setOnGround(Risen.onGround());
        Body.setDeltaMovement(Risen.getDeltaMovement());
        Body.attackAnim = Risen.attackAnim;
        Body.oAttackAnim = Risen.oAttackAnim;
        Body.swinging = Risen.swinging;
        Body.swingTime = Risen.swingTime;
        Body.hurtTime = Risen.hurtTime;
        Body.hurtDuration = Risen.hurtDuration;
        Body.deathTime = Risen.deathTime;
        Body.setSprinting(false);
        Body.setShiftKeyDown(false);
        if (Found.Ticks) {
            try {
                Body.tick();
            } catch (RuntimeException Problem) {
                Found.Ticks = false;
            }
            place(Body, Risen);
        }
        if (!Found.Ticks) {
            double Dx = Body.getX() - Body.xo;
            double Dz = Body.getZ() - Body.zo;
            Body.walkAnimation.update(Math.min((float) Math.sqrt(Dx * Dx + Dz * Dz) * 4.0f, 1.0f), 0.4f);
        }
        if (Body.isRemoved() || Body.getHealth() <= 0.0f) {
            Found.Broken = true;
        }
        Found.Tick = Risen.tickCount;
    }

    private static void place(LivingEntity Body, Scp049Cured Risen) {
        Body.setPos(Risen.getX(), Risen.getY(), Risen.getZ());
        Body.setYRot(Risen.getYRot());
        Body.yRotO = Risen.yRotO;
        Body.setXRot(Risen.getXRot());
        Body.xRotO = Risen.xRotO;
        Body.yBodyRot = Risen.yBodyRot;
        Body.yBodyRotO = Risen.yBodyRotO;
        Body.yHeadRot = Risen.yHeadRot;
        Body.yHeadRotO = Risen.yHeadRotO;
    }

    public static void reset() {
        Mimics.clear();
    }
}
