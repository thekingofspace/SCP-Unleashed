package net.rai.scp_unleashed.scp5001.mekhane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneTimeline;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneFxPayload;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneSitePayload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MekhaneClientState {
    public static final double ShakeNear = 120.0;
    public static final double ShakeFar = 420.0;
    public static final double TintReach = 96.0;
    public static final int ShockLife = 24;
    public static final float ShockReach = 34.0f;
    private static MekhaneProtocol.Stage Stage = MekhaneProtocol.Stage.Idle;
    private static int Elapsed;
    private static int Ticks;
    private static BlockPos Emitter = BlockPos.ZERO;
    private static double Hover;
    private static float Rumble;
    private static float Flash;
    private static float FlashO;
    private static float Alarm;
    private static float Tint;
    private static float TintO;
    private static float Shake;
    private static final List<Shock> Shocks = new ArrayList<>();

    public record Shock(Vec3 At, float Power, int Born) {
    }

    private MekhaneClientState() {
    }

    public static void site(MekhaneSitePayload Payload) {
        MekhaneProtocol.Stage[] All = MekhaneProtocol.Stage.values();
        Stage = All[Mth.clamp(Payload.Stage(), 0, All.length - 1)];
        Elapsed = Payload.Elapsed();
        Emitter = Payload.Emitter();
        Hover = Payload.Hover();
    }

    public static void reset() {
        Stage = MekhaneProtocol.Stage.Idle;
        Elapsed = 0;
        Rumble = 0.0f;
        Flash = 0.0f;
        FlashO = 0.0f;
        Tint = 0.0f;
        TintO = 0.0f;
        Shocks.clear();
    }

    public static MekhaneProtocol.Stage stage() {
        return Stage;
    }

    public static int elapsed() {
        return Elapsed;
    }

    public static BlockPos emitter() {
        return Emitter;
    }

    public static double hover() {
        return Hover;
    }

    public static int ticks() {
        return Ticks;
    }

    public static float beam(float Partial) {
        return MekhaneTimeline.beam(Stage, Elapsed);
    }

    public static float flash(float Partial) {
        return Mth.lerp(Partial, FlashO, Flash);
    }

    public static float tint(float Partial) {
        return Mth.lerp(Partial, TintO, Tint);
    }

    public static float shake() {
        return Shake;
    }

    public static List<Shock> shocks() {
        return Shocks;
    }

    private static float falloff(double Distance) {
        return (float) Mth.clamp(1.0 - (Distance - ShakeNear) / (ShakeFar - ShakeNear), 0.0, 1.0);
    }

    public static void fx(MekhaneFxPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        Level World = Client.level;
        Player Viewer = Client.player;
        if (World == null || Viewer == null) {
            return;
        }
        Vec3 At = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        double Distance = Viewer.position().distanceTo(At);
        float Near = falloff(Distance);
        switch (Payload.Kind()) {
            case MekhaneFxPayload.Shock -> {
                Shocks.add(new Shock(At, Payload.Power(), Ticks));
                Rumble = Math.max(Rumble, 0.6f * Payload.Power() * Near);
                burst(World, At, Payload.Power(), false);
            }
            case MekhaneFxPayload.Sparks -> burst(World, At, Payload.Power(), true);
            case MekhaneFxPayload.Flash -> Flash = Math.min(1.0f, Math.max(Flash, Payload.Power() * (0.35f + 0.65f * Near)));
            case MekhaneFxPayload.Rumble -> Rumble = Math.max(Rumble, Payload.Power() * Near);
            case MekhaneFxPayload.Alarm -> Alarm = 1.0f;
            default -> {
            }
        }
    }

    private static void burst(Level World, Vec3 At, float Power, boolean Debris) {
        RandomSource Random = World.random;
        int Count = (int) (24 * Power);
        for (int Index = 0; Index < Count; Index++) {
            double Vx = (Random.nextDouble() - 0.5) * 1.6 * Power;
            double Vy = (Random.nextDouble() - 0.3) * 1.2 * Power;
            double Vz = (Random.nextDouble() - 0.5) * 1.6 * Power;
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, At.x, At.y, At.z, Vx, Vy, Vz);
            World.addParticle(ParticleTypes.END_ROD, At.x, At.y, At.z, Vx * 0.6, Vy * 0.6, Vz * 0.6);
            if (Debris && Index % 3 == 0) {
                ItemStack Piece = new ItemStack(Index % 2 == 0 ? Items.GOLD_NUGGET : Items.IRON_NUGGET);
                World.addParticle(new ItemParticleOption(ParticleTypes.ITEM, Piece), At.x, At.y, At.z, Vx * 0.5, Vy * 0.4, Vz * 0.5);
            }
        }
        if (!Debris) {
            for (int Index = 0; Index < 16; Index++) {
                double Angle = Index / 16.0 * Math.PI * 2.0;
                World.addParticle(ParticleTypes.FLASH, At.x + Math.cos(Angle) * 4.0, At.y, At.z + Math.sin(Angle) * 4.0, 0.0, 0.0, 0.0);
            }
        }
    }

    public static void tick(Minecraft Client) {
        Ticks++;
        FlashO = Flash;
        TintO = Tint;
        if (Client.level == null || Client.player == null) {
            reset();
            return;
        }
        if (Stage != MekhaneProtocol.Stage.Idle) {
            Elapsed++;
        }
        Flash = Math.max(0.0f, Flash * 0.86f - 0.01f);
        Rumble = Math.max(0.0f, Rumble - 0.02f);
        Alarm = Math.max(0.0f, Alarm - 0.025f);
        Player Viewer = Client.player;
        double Distance = Math.sqrt(Viewer.distanceToSqr(Emitter.getX() + 0.5, Viewer.getY(), Emitter.getZ() + 0.5));
        float Ground = 0.0f;
        if (Stage == MekhaneProtocol.Stage.Summon) {
            Ground = 0.25f + 0.55f * MekhaneTimeline.smooth(Elapsed / (float) MekhaneTimeline.SummonTicks);
        } else if (Stage == MekhaneProtocol.Stage.Duel && Elapsed < MekhaneTimeline.Banish) {
            Ground = 0.12f;
        }
        Ground *= falloff(Distance);
        float Comfort = Client.options.screenEffectScale().get().floatValue();
        Shake = Math.max(Rumble, Ground) * Comfort;
        boolean Inside = Distance < TintReach && Viewer.getY() < Hover - 24.0;
        boolean Active = Stage == MekhaneProtocol.Stage.Summon || Stage == MekhaneProtocol.Stage.Duel;
        float Wanted = Inside && Active ? 1.0f : 0.0f;
        Tint += Mth.clamp(Wanted - Tint, -0.03f, 0.03f);
        Iterator<Shock> Each = Shocks.iterator();
        while (Each.hasNext()) {
            if (Ticks - Each.next().Born() > ShockLife) {
                Each.remove();
            }
        }
    }

    public static float alarm() {
        return Alarm;
    }
}
