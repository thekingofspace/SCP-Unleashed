package net.rai.scp_unleashed.scp2317;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp001scarlet.ScarletWorld;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp2317Ritual {
    public static final double FullTurn = Math.PI * 2.0;
    public static final double WrongWay = Math.PI / 2.0;
    public static final int GlowEvery = 10;
    public static final String MessagePrefix = "message.scp_unleashed.scp2317.";
    private static final Map<UUID, Attempt> Attempts = new ConcurrentHashMap<>();

    public enum Outcome {
        None,
        Armed,
        Refused,
        Turned,
        Done,
        Failed
    }

    public static final class Attempt {
        public long ArmedAt;
        public int Stage;
        public double Sweep;
        public double LastAngle = Double.NaN;
        public double StartAngle = Double.NaN;
        public int Backward;

        public Attempt(long When) {
            ArmedAt = When;
        }
    }

    private Scp2317Ritual() {
    }

    public static long now(MinecraftServer Server) {
        return Server.overworld().getGameTime();
    }

    public static boolean cooling(MinecraftServer Server) {
        return !ScarletMeter.riteReady(Server);
    }

    public static void resetCooldown(MinecraftServer Server) {
        ScarletWorld.get(Server).lastRite(Long.MIN_VALUE);
    }

    @Nullable
    public static Attempt attempt(UUID Id) {
        return Attempts.get(Id);
    }

    public static void forget(UUID Id) {
        Attempts.remove(Id);
    }

    public static void forgetAll() {
        Attempts.clear();
    }

    public static boolean active(MinecraftServer Server) {
        return !Attempts.isEmpty();
    }

    public static boolean meaningful(MinecraftServer Server) {
        return ScarletMeter.climbing(ScarletState.get(Server).phase());
    }

    public static Outcome died(LivingEntity Victim, DamageSource Source) {
        if (!(Victim instanceof Chicken) || !(Victim.level() instanceof ServerLevel World) || !Scp2317World.inside(World)) {
            return Outcome.None;
        }
        ServerPlayer Killer = Source.getEntity() instanceof ServerPlayer Direct ? Direct : Victim.getKillCredit() instanceof ServerPlayer Credit ? Credit : null;
        if (Killer == null || Victim.position().distanceTo(Scp2317World.vase()) > Scp2317Config.reach() + 0.75) {
            return Outcome.None;
        }
        return offer(Killer, World);
    }

    public static Outcome offer(ServerPlayer Killer, ServerLevel World) {
        MinecraftServer Server = World.getServer();
        Vec3 Mouth = Scp2317World.vase();
        if (cooling(Server) || !meaningful(Server)) {
            Scp2317Sounds.play(World, Mouth, List.of(Scp2317Registry.Refuse), SoundSource.BLOCKS, 0.8f, 0.8f);
            World.sendParticles(ParticleTypes.SMOKE, Mouth.x, Mouth.y, Mouth.z, 6, 0.15, 0.1, 0.15, 0.01);
            return Outcome.Refused;
        }
        Attempts.put(Killer.getUUID(), new Attempt(now(Server)));
        Scp2317Sounds.play(World, Mouth, Scp2317Registry.AcceptLayers, SoundSource.BLOCKS, 1.0f, 0.7f);
        World.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.05f, 0.06f), 1.2f), Mouth.x, Mouth.y + 0.2, Mouth.z, 24, 0.2, 0.25, 0.2, 0.01);
        glow(World, Scp2317VaseBlock.Awake);
        tell(Killer, "accepted");
        return Outcome.Armed;
    }

    public static Outcome track(ServerPlayer User) {
        Attempt Current = Attempts.get(User.getUUID());
        if (Current == null) {
            return Outcome.None;
        }
        Outcome Result = observe(User, User.position());
        Attempt After = Attempts.get(User.getUUID());
        if (After != null) {
            Scp2317RitualGuide.show(User, After);
        }
        return Result;
    }

    public static Outcome observe(ServerPlayer User, Vec3 Spot) {
        Attempt Current = Attempts.get(User.getUUID());
        if (Current == null) {
            return Outcome.None;
        }
        MinecraftServer Server = User.server;
        Vec3 Centre = Scp2317World.centre();
        double Dx = Spot.x - Centre.x;
        double Dz = Spot.z - Centre.z;
        double Radius = Math.sqrt(Dx * Dx + Dz * Dz);
        if (now(Server) - Current.ArmedAt > Scp2317Config.window() || !Scp2317World.inside(User) || !User.isAlive() || Radius > Scp2317Plan.LeaveRadius) {
            return fail(User);
        }
        if (Radius < Scp2317Plan.WalkRadius) {
            Current.LastAngle = Double.NaN;
            return Outcome.None;
        }
        double Angle = Math.atan2(Dz, Dx);
        if (Double.isNaN(Current.StartAngle)) {
            Current.StartAngle = Angle;
        }
        if (!Double.isNaN(Current.LastAngle)) {
            double Step = Angle - Current.LastAngle;
            while (Step > Math.PI) {
                Step -= FullTurn;
            }
            while (Step < -Math.PI) {
                Step += FullTurn;
            }
            Current.Sweep += Step;
        }
        Current.LastAngle = Angle;
        if (Current.Stage == 0) {
            if (Current.Sweep < -WrongWay) {
                return fail(User);
            }
            if (Current.Sweep >= FullTurn) {
                Current.Stage = 1;
                Current.Sweep = 0.0;
                Current.StartAngle = Angle;
                ServerLevel World = User.serverLevel();
                World.playSound(null, Spot.x, Spot.y, Spot.z, Scp2317Registry.Turn.get(), SoundSource.AMBIENT, 0.7f, 0.8f);
                Scp2317RitualGuide.turned(User);
                return Outcome.Turned;
            }
            return Outcome.None;
        }
        if (Current.Sweep > WrongWay) {
            return fail(User);
        }
        if (Current.Sweep <= -FullTurn) {
            return succeed(User);
        }
        return Outcome.None;
    }

    public static Outcome fail(ServerPlayer User) {
        Attempts.remove(User.getUUID());
        if (Attempts.isEmpty()) {
            ServerLevel World = Scp2317World.level(User.server);
            if (World != null) {
                glow(World, cooling(User.server) ? Scp2317VaseBlock.Dark : Scp2317VaseBlock.Idle);
            }
        }
        return Outcome.Failed;
    }

    public static Outcome succeed(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Attempts.remove(User.getUUID());
        if (!ScarletMeter.rite(Server)) {
            return fail(User);
        }
        ServerLevel World = Scp2317World.level(Server);
        if (World != null) {
            flare(World);
            glow(World, Scp2317VaseBlock.Dark);
        }
        tell(User, "exhale");
        return Outcome.Done;
    }

    public static void flare(ServerLevel World) {
        DustParticleOptions Ember = new DustParticleOptions(new Vector3f(0.8f, 0.12f, 0.06f), 2.0f);
        for (int Index = 0; Index < Scp2317Plan.Pillars; Index++) {
            int[] Centre = Scp2317Plan.pillarCentre(Index, Scp2317Plan.RingRadius);
            double X = Scp2317World.baseX() + Centre[0] + 0.5;
            double Z = Scp2317World.baseZ() + Centre[1] + 0.5;
            double Top = Scp2317Plan.Ground + Scp2317Plan.pillarHeight(Index) + 1.2;
            World.sendParticles(Ember, X, Top, Z, 30, 0.6, 0.8, 0.6, 0.02);
            World.sendParticles(ParticleTypes.FLAME, X, Top, Z, 12, 0.4, 0.4, 0.4, 0.01);
        }
        Vec3 Centre = Scp2317World.centre();
        Scp2317Sounds.play(World, Centre, Scp2317Registry.ExhaleLayers, SoundSource.AMBIENT, 4.0f, 0.7f);
    }

    public static void glow(ServerLevel World, int Value) {
        BlockPos Pos = Scp2317World.vasePos();
        if (!World.isLoaded(Pos)) {
            return;
        }
        BlockState Here = World.getBlockState(Pos);
        if (Here.getBlock() instanceof Scp2317VaseBlock && Here.getValue(Scp2317VaseBlock.Glow) != Value) {
            World.setBlock(Pos, Here.setValue(Scp2317VaseBlock.Glow, Value), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Scp2317World.level(Server);
        if (World == null) {
            return;
        }
        if (Server.getTickCount() % GlowEvery == 0) {
            int Wanted = cooling(Server) || !meaningful(Server) ? Scp2317VaseBlock.Dark : Attempts.isEmpty() ? Scp2317VaseBlock.Idle : Scp2317VaseBlock.Awake;
            glow(World, Wanted);
        }
    }

    public static void tell(ServerPlayer User, String Key) {
        User.displayClientMessage(Component.translatable(MessagePrefix + Key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
    }
}
