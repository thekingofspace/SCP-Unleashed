package net.rai.scp_unleashed.z0003.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidGait;
import net.rai.scp_unleashed.z0003.Z0003Frozen;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Statue;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class Z0003StatueClient {
    public static final int FreshTicks = 3;
    public static final float StrideSpeed = 0.08f;
    public static final float RunSpeed = 0.64f;
    public static final double GaitScale = 4.0 * HumanoidGait.WalkCycle;
    public static final double FrozenSensitivity = -1.0 / 3.0;
    public static final int FlashTicks = 12;

    private static final Int2ObjectMap<Z0003Frozen> Frozen = new Int2ObjectOpenHashMap<>();
    private static boolean LocalStatue;
    private static boolean LocalCracking;
    private static int Flash;

    private Z0003StatueClient() {
    }

    public static boolean tick(Entity Subject) {
        int Id = Subject.getId();
        Z0003Statue Statue = Z0003Petrify.statueOf(Subject);
        if (Statue == null || !(Subject instanceof LivingEntity Living)) {
            Frozen.remove(Id);
            return false;
        }
        if (Subject == Minecraft.getInstance().player) {
            if (!Frozen.containsKey(Id)) {
                Frozen.put(Id, Z0003Frozen.held(Living, Statue, Subject.tickCount));
            }
            return false;
        }
        Z0003Frozen Shot = Frozen.get(Id);
        if (Shot == null) {
            Subject.tickCount--;
            if (Subject.tickCount <= FreshTicks) {
                pose(Living, Statue);
            }
            Living.hurtTime = 0;
            Shot = Z0003Frozen.capture(Living).still();
            Frozen.put(Id, Shot);
        }
        Subject.tickCount = Shot.Age();
        follow(Subject);
        Living.hurtTime = 0;
        return true;
    }

    @Nullable
    public static Z0003Frozen snapshot(Entity Subject) {
        return Frozen.get(Subject.getId());
    }

    public static void pose(LivingEntity Living, Z0003Statue Statue) {
        Living.yBodyRot = Statue.BodyYaw();
        Living.yBodyRotO = Statue.BodyYaw();
        Living.yHeadRot = Statue.HeadYaw();
        Living.yHeadRotO = Statue.HeadYaw();
        Living.setXRot(Statue.Pitch());
        Living.xRotO = Statue.Pitch();
        WalkAnimationState Walk = Living.walkAnimation;
        float Speed = Statue.WalkSpeed();
        Walk.update(Statue.WalkPosition() - Walk.position() - Speed, 1.0f);
        Walk.update(Speed, 1.0f);
        Living.attackAnim = Statue.Swing();
        Living.oAttackAnim = Statue.Swing();
        if (Living instanceof DClassLike Body) {
            HumanoidGait Gait = Body.gait();
            if (Gait != null) {
                double Cycle = Statue.WalkPosition() / GaitScale;
                Gait.hold(Cycle - Math.floor(Cycle), Speed > StrideSpeed, Statue.Running() || Speed > RunSpeed);
            }
        }
    }

    private static void follow(Entity Subject) {
        Vec3 Base = Subject.getPositionCodec().getBase();
        Subject.setOldPosAndRot();
        if (Base.distanceToSqr(Subject.position()) > 1.0E-6 && Base.lengthSqr() > 0.0) {
            Subject.setPos(Base);
        }
    }

    public static void forget(int Id) {
        Frozen.remove(Id);
    }

    public static void reset() {
        Frozen.clear();
        LocalStatue = false;
        LocalCracking = false;
        Flash = 0;
    }

    @Nullable
    public static Z0003Statue local() {
        LocalPlayer User = Minecraft.getInstance().player;
        return User == null ? null : Z0003Petrify.statueOf(User);
    }

    public static boolean isLocked() {
        return local() != null;
    }

    public static void clientTick(Minecraft Game) {
        Z0003Statue Statue = local();
        boolean Now = Statue != null;
        if (LocalStatue && !Now && LocalCracking) {
            Flash = FlashTicks;
        } else if (Flash > 0) {
            Flash--;
        }
        LocalStatue = Now;
        LocalCracking = Now && Statue.cracking();
        if (Now) {
            consumeKeys(Game);
        }
    }

    public static float flash(float Partial) {
        return Flash <= 0 ? 0.0f : Mth.clamp((Flash - Partial) / FlashTicks, 0.0f, 1.0f);
    }

    public static void holdLocal(LocalPlayer User) {
        Z0003Statue Statue = Z0003Petrify.statueOf(User);
        if (Statue == null || User.isPassenger()) {
            return;
        }
        Vec3 Motion = User.getDeltaMovement();
        User.setDeltaMovement(0.0, Math.min(0.0, Motion.y), 0.0);
        if (Math.abs(User.getX() - Statue.X()) > 1.0E-4 || Math.abs(User.getZ() - Statue.Z()) > 1.0E-4 || User.getY() > Statue.Y() + 1.0E-4) {
            User.setPos(Statue.X(), Math.min(User.getY(), Statue.Y()), Statue.Z());
        }
        User.setSprinting(false);
    }

    public static void steer(Input Controls) {
        Z0003Statue Statue = local();
        if (Statue == null) {
            return;
        }
        Controls.leftImpulse = 0.0f;
        Controls.forwardImpulse = 0.0f;
        Controls.up = false;
        Controls.down = false;
        Controls.left = false;
        Controls.right = false;
        Controls.jumping = false;
        Controls.shiftKeyDown = Statue.Crouching();
    }

    public static void consumeKeys(Minecraft Game) {
        Options Settings = Game.options;
        Set<KeyMapping> Allowed = Set.of(Settings.keyChat, Settings.keyCommand, Settings.keyPlayerList, Settings.keyScreenshot, Settings.keyFullscreen);
        for (KeyMapping Mapping : Settings.keyMappings) {
            if (Allowed.contains(Mapping) || Mapping == Settings.keyShift) {
                continue;
            }
            boolean Pressed = Mapping.consumeClick();
            while (Pressed) {
                Pressed = Mapping.consumeClick();
            }
            Mapping.setDown(false);
        }
    }
}
