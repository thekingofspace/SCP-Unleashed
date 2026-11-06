package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidStyle;

public final class Scp610RaidStyle implements RaidStyle {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flesh_horde");
    public static final Scp610RaidStyle Instance = new Scp610RaidStyle();
    public static final double RaidPower = 120.0;
    public static final int WarningTicks = 600;
    public static final long DurationTicks = 6000L;
    public static final long CooldownTicks = 36000L;
    public static final int MaxAlive = 8;
    public static final int MinHorde = 3;
    public static final int MaxHorde = 24;
    public static final double PowerPerUnit = 120.0;
    public static final int EvolvedEvery = 5;
    public static final int WaveSeconds = 12;
    public static final double RingMin = 12.0;
    public static final double RingMax = 22.0;
    public static final String PendingKey = "Pending";
    public static final String SpawnedKey = "Spawned";
    public static final String SecondsKey = "Seconds";
    public static final String ApexKey = "Apex";

    private Scp610RaidStyle() {
    }

    public static boolean ready(FactionState State) {
        return State.phase().active() && State.power() >= RaidPower && State.countSites(Scp610Hive.Kind) > 0;
    }

    public static int hordeSize(double Power) {
        return Mth.clamp(MinHorde + (int) (Power / PowerPerUnit), MinHorde, MaxHorde);
    }

    @Override
    public ResourceLocation id() {
        return Id;
    }

    @Override
    public int warningTicks() {
        return WarningTicks;
    }

    @Override
    public long durationTicks() {
        return DurationTicks;
    }

    @Override
    public long cooldownTicks() {
        return CooldownTicks;
    }

    @Override
    public int maxAlive() {
        return MaxAlive;
    }

    @Override
    public boolean allowsDimension(ResourceKey<Level> Dimension) {
        return true;
    }

    @Override
    public String noticeKey(String Event) {
        return "raid.scp_unleashed.flesh." + Event;
    }

    @Override
    public double heading(ServerLevel World, FactionType Faction, FactionState State, BlockPos Target) {
        BlockPos Nearest = null;
        double Gap = Double.MAX_VALUE;
        for (Scp610Hive Hive : Scp610Hives.list(World.getServer())) {
            if (!Hive.Site.dimension().equals(World.dimension())) {
                continue;
            }
            double Found = Hive.anchor().distSqr(Target);
            if (Found < Gap) {
                Gap = Found;
                Nearest = Hive.anchor();
            }
        }
        if (Nearest == null) {
            return World.random.nextDouble() * Mth.TWO_PI;
        }
        return Math.atan2(Nearest.getZ() - Target.getZ(), Nearest.getX() - Target.getX()) + (World.random.nextDouble() - 0.5) * 0.8;
    }

    @Override
    public void warn(RaidContext Context) {
        Vec3 Centre = Context.Current().targetCentre();
        double Heading = Context.Current().heading();
        Vec3 From = Centre.add(Math.cos(Heading) * 24.0, 0.0, Math.sin(Heading) * 24.0);
        for (ServerPlayer User : Context.defenders(96.0)) {
            User.connection.send(new ClientboundSoundPacket(Scp610Sounds.RaidWarning, SoundSource.HOSTILE, From.x, From.y, From.z,
                    2.0f, 0.7f, Context.random().nextLong()));
        }
    }

    @Override
    public boolean begin(RaidContext Context) {
        CompoundTag Data = Context.data();
        Data.putInt(PendingKey, hordeSize(Context.State().power()));
        Data.putInt(SpawnedKey, 0);
        Data.putInt(SecondsKey, 0);
        Data.putBoolean(ApexKey, Context.State().phase() == FactionPhase.Apex);
        Context.changed();
        wave(Context);
        return true;
    }

    @Override
    public void tick(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Seconds = Data.getInt(SecondsKey) + 1;
        Data.putInt(SecondsKey, Seconds);
        if (Seconds % WaveSeconds == 0) {
            wave(Context);
        }
        for (Mob Raider : Context.loadedRaiders()) {
            if (Raider.getTarget() == null) {
                Vec3 Aim = Context.Current().targetCentre();
                Raider.getNavigation().moveTo(Aim.x, Aim.y, Aim.z, 1.1);
            }
        }
        Context.changed();
    }

    private void wave(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Pending = Data.getInt(PendingKey);
        int Room = Context.room();
        ServerLevel World = Context.World();
        BlockPos Target = Context.Current().target();
        double Heading = Context.Current().heading();
        BlockPos Front = BlockPos.containing(Target.getX() + Math.cos(Heading) * 16.0, Target.getY(), Target.getZ() + Math.sin(Heading) * 16.0);
        int Count = Math.min(Pending, Math.min(Room, 4));
        for (int Index = 0; Index < Count; Index++) {
            BlockPos Spot = RaidGround.standAround(World, Front, 0.0, RingMax - RingMin, Target.getY(), 10, Context.random(), 12, false);
            if (Spot == null) {
                continue;
            }
            int Made = Data.getInt(SpawnedKey);
            Scp610Form Form = Data.getBoolean(ApexKey) && Made % EvolvedEvery == EvolvedEvery - 1 ? evolved(Context) : Scp610Faction.pickRecruit(Context.random());
            Scp610Creature Raider = Scp610Spawning.raise(World, Form, Vec3.atBottomCenterOf(Spot), (float) Math.toDegrees(Heading) + 90.0f, true);
            if (Raider == null) {
                continue;
            }
            Context.enlist(Raider, RaidMark.Raider);
            Data.putInt(SpawnedKey, Made + 1);
            Pending--;
        }
        Data.putInt(PendingKey, Pending);
        Context.changed();
    }

    private static Scp610Form evolved(RaidContext Context) {
        return switch (Context.random().nextInt(6)) {
            case 0 -> Scp610Form.Amalgam;
            case 1 -> Scp610Form.Colossus;
            case 2 -> Scp610Form.SiegeBrute;
            case 3 -> Scp610Form.Stalker;
            case 4 -> Scp610Form.Spitter;
            default -> Scp610Form.Herald;
        };
    }

    @Override
    public boolean exhausted(RaidContext Context) {
        return Context.data().getInt(PendingKey) <= 0;
    }

    @Override
    public void dismiss(ServerLevel World, Mob Raider) {
        if (Raider instanceof Scp610Creature Creature) {
            Creature.collapse();
        } else {
            Raider.discard();
        }
    }
}
