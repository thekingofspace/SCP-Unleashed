package net.rai.scp_unleashed.scp5001.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001ChamberCoreBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001LaserEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001RodEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ActionPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001BurstPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001OpenPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001StatePayload;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Spawning;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp5001Event {
    public static final int StateEvery = 20;
    public static final int AlarmEvery = 30;
    public static final int ReachSquared = 64;
    public static final int GeneratorReach = 24;
    public static final int BlastTicks = 60;
    public static final int SweepEvery = 100;
    public static final int MissingLimit = 3;
    public static final double LatchShare = 0.72;
    public static final int BurstFlashReach = 160;
    public static final float BurstDamage = 8.0f;
    public static final Scp610Form[][] SwarmPools = {
            {Scp610Form.Shambler, Scp610Form.Crawler, Scp610Form.Villager, Scp610Form.Pig, Scp610Form.Cow, Scp610Form.Sheep},
            {Scp610Form.Shambler, Scp610Form.Crawler, Scp610Form.Hulk, Scp610Form.Stalker, Scp610Form.Spitter, Scp610Form.Horse},
            {Scp610Form.Hulk, Scp610Form.Stalker, Scp610Form.Spitter, Scp610Form.Amalgam, Scp610Form.SiegeBrute, Scp610Form.Herald}};
    private static final Map<UUID, Integer> Missing = new HashMap<>();
    @Nullable
    private static Scp5001Vessel Cached;

    private Scp5001Event() {
    }

    public static boolean mine(Scp5001State.Phase Phase) {
        return Phase.ordinal() <= Scp5001State.Phase.Resetting.ordinal();
    }

    public static boolean live(Scp5001State.Phase Phase) {
        return Phase == Scp5001State.Phase.Rising || Phase == Scp5001State.Phase.Overdrive || Phase == Scp5001State.Phase.Purging;
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        Scp5001State State = Scp5001State.get(Server);
        Scp5001EventData Data = Scp5001EventData.get(Server);
        long Now = World.getGameTime();
        Scp5001State.Phase Current = State.phase();
        Scp5001State.Phase Before = Data.lastSeen();
        if (Current != Before) {
            transition(World, State, Data, Before, Current, Now);
            Data.lastSeen(Current);
            broadcast(World);
        }
        switch (Current) {
            case Dormant -> dormant(World, State, Data, Now);
            case Unlocked -> unlocked(World, State, Data, Now);
            case Rising -> rising(World, State, Data, Now);
            case Overdrive -> overdrive(World, State, Data, Now);
            case Purging -> purging(World, State, Data, Now);
            case Resetting -> resetting(World, State, Data, Now);
            default -> {
            }
        }
        Scp5001Exposure.tick(World, State, Now);
        if (Now % StateEvery == 0) {
            broadcast(World);
        }
    }

    private static void transition(ServerLevel World, Scp5001State State, Scp5001EventData Data, Scp5001State.Phase Before, Scp5001State.Phase Current, long Now) {
        if (Current == Scp5001State.Phase.Dormant && Before != Scp5001State.Phase.Dormant) {
            Data.cooldownEnd(Now + Scp5001EventConfig.cooldownTicks());
            Data.rodsSpent(true);
            Data.clearOverdrive();
            Data.clearSwarm();
            State.lift(0.0f);
            State.overdriven(0);
        }
        if (Current == Scp5001State.Phase.Rising && Before == Scp5001State.Phase.Unlocked) {
            Data.riseStart(Now);
            Data.nextWave(Now + 40);
            for (BlockPos Rod : rods(World, State, Data)) {
                play(World, Rod, Scp5001EventRegistry.RodExtend, 2.0f, 0.9f);
            }
        }
        if (Current == Scp5001State.Phase.Overdrive) {
            Data.clearSwarm();
            Data.nextSwarm(Now + 60);
        }
    }

    private static void dormant(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        if (Data.cooldownEnd() > 0 && Now >= Data.cooldownEnd()) {
            Data.cooldownEnd(0);
            Data.rodsSpent(false);
            broadcast(World);
        }
    }

    private static void unlocked(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        long Elapsed = Now - State.phaseTick();
        if (Elapsed % AlarmEvery == 0) {
            alarm(World, State, Data);
        }
        if (Elapsed >= Scp5001EventConfig.alarmTicks()) {
            State.phase(Scp5001State.Phase.Rising, Now);
        }
    }

    public static boolean latched(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        return !Data.rodsSpent() && !rods(World, State, Data).isEmpty() && Now - Data.riseStart() >= Scp5001EventConfig.rodLatchTicks();
    }

    public static double riseRate(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        double Rate = 1.0 / Scp5001EventConfig.riseTicks();
        if (!latched(World, State, Data, Now)) {
            Rate /= 1.0 - Scp5001EventConfig.rodDrag();
        }
        return Rate * Math.max(0.05, 1.0 - Scp5001EventConfig.overdriveSlow() * State.overdriven());
    }

    private static void rising(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        if (Now - Data.riseStart() == Scp5001EventConfig.rodLatchTicks() && !Data.rodsSpent()) {
            for (BlockPos Rod : rods(World, State, Data)) {
                play(World, Rod, Scp5001EventRegistry.RodLatch, 2.5f, 0.8f + World.random.nextFloat() * 0.2f);
            }
        }
        State.lift((float) (State.lift() + riseRate(World, State, Data, Now)));
        if (Now % 80 == 0 && latched(World, State, Data, Now)) {
            for (BlockPos Rod : rods(World, State, Data)) {
                play(World, Rod, Scp5001EventRegistry.RodStrain, 1.5f, 0.7f + State.lift() * 0.5f);
            }
        }
        waves(World, State, Data, Now, 1.0);
        if (State.lift() >= 1.0f) {
            burst(World, State, Data, Now);
            return;
        }
        int Generators = generators(World, State, Data).size();
        if (Generators > 0 && State.overdriven() >= Generators) {
            State.phase(Scp5001State.Phase.Overdrive, Now);
        }
    }

    private static void overdrive(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        waves(World, State, Data, Now, 0.5);
        if (abandoned(World, State, Data, Now)) {
            return;
        }
        sweep(World, Data, Now);
        if (!Data.swarm().isEmpty()) {
            return;
        }
        if (Data.swarmWave() >= Scp5001EventConfig.swarmWaves()) {
            State.phase(Scp5001State.Phase.Purging, Now);
            play(World, State.core(), Scp5001EventRegistry.ConsoleBeep, 2.0f, 0.7f);
            return;
        }
        if (Data.nextSwarm() == 0) {
            Data.nextSwarm(Now + Scp5001EventConfig.swarmDelayTicks());
            return;
        }
        if (Now >= Data.nextSwarm()) {
            spawnWave(World, State, Data, Data.swarmWave());
            Data.swarmWave(Data.swarmWave() + 1);
            Data.nextSwarm(0);
        }
    }

    private static void purging(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        abandoned(World, State, Data, Now);
    }

    private static void resetting(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        State.lift((float) (State.lift() - 1.0 / Scp5001EventConfig.resetTicks()));
        if (Now - Data.resetStart() == BlastTicks) {
            Scp5001Vessel Vessel = vessel(World, State);
            if (Vessel != null) {
                World.playSound(null, Vessel.getX(), Vessel.getY() + Scp5001Vessel.CentreHeight, Vessel.getZ(), Scp5001EventRegistry.VesselSettle.get(), SoundSource.BLOCKS, 5.0f, 0.8f);
            }
        }
        if (State.lift() <= 0.0f && Now - Data.resetStart() >= BlastTicks) {
            State.phase(Scp5001State.Phase.Dormant, Now);
        }
    }

    private static boolean abandoned(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        if (!playersInside(World, State).isEmpty()) {
            Data.abandoned(0);
            return false;
        }
        Data.abandoned(Data.abandoned() + 1);
        if (Data.abandoned() < Scp5001EventConfig.abandonTicks()) {
            return false;
        }
        Data.abandoned(0);
        Data.clearOverdrive();
        State.overdriven(0);
        Data.riseStart(Now);
        State.phase(Scp5001State.Phase.Rising, Now);
        Data.lastSeen(Scp5001State.Phase.Rising);
        return true;
    }

    private static void waves(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now, double Scale) {
        if (Now < Data.nextWave()) {
            return;
        }
        Data.nextWave(Now + Scp5001EventConfig.waveTicks());
        int Serial = Data.nextWaveSerial();
        Scp5001Vessel Vessel = vessel(World, State);
        Vec3 From = Vessel != null ? Vessel.centre() : Vec3.atCenterOf(State.core());
        if (Vessel != null) {
            Vessel.pulse(Serial);
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp5001EventRegistry.WaveLayers) {
            World.playSound(null, From.x, From.y, From.z, Layer.get(), SoundSource.HOSTILE, 8.0f, 0.9f + World.random.nextFloat() * 0.1f);
        }
        Scp5001Exposure.pulse(World, State, Scale);
    }

    public static void burst(ServerLevel World, Scp5001State State, Scp5001EventData Data, long Now) {
        Scp5001Vessel Vessel = vessel(World, State);
        Vec3 Centre = Vessel != null ? Vessel.centre() : Vec3.atCenterOf(State.core()).add(0.0, 8.0, 0.0);
        State.phase(Scp5001State.Phase.Xk1, Now);
        Data.lastSeen(Scp5001State.Phase.Xk1);
        Data.rodsSpent(true);
        Data.clearOverdrive();
        State.overdriven(0);
        if (Vessel != null) {
            Vessel.burst();
            Vessel.playBurstEffects(World);
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp5001EventRegistry.BurstLayers) {
            World.playSound(null, Centre.x, Centre.y, Centre.z, Layer.get(), SoundSource.HOSTILE, 16.0f, 0.7f);
        }
        float Radius = Scp5001EventConfig.burstRadius();
        Scp5001BurstPayload Flash = new Scp5001BurstPayload(Centre.x, Centre.y, Centre.z, Radius);
        for (ServerPlayer Each : World.players()) {
            if (Each.position().distanceToSqr(Centre) < BurstFlashReach * BurstFlashReach) {
                PacketDistributor.sendToPlayer(Each, Flash);
            }
        }
        if (Radius > 0.0f) {
            AABB Area = new AABB(Centre, Centre).inflate(Radius);
            for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found.isAlive() && !(Found instanceof Player User && (User.isCreative() || User.isSpectator())))) {
                double Distance = Each.position().distanceTo(Centre);
                if (Distance > Radius) {
                    continue;
                }
                double Push = 2.2 * (1.0 - Distance / Radius) + 0.4;
                Vec3 Away = Each.position().subtract(Centre).normalize();
                Each.push(Away.x * Push, 0.5 + Push * 0.3, Away.z * Push);
                Each.hurtMarked = true;
                Each.hurt(World.damageSources().explosion(null, null), (float) (BurstDamage * (1.0 - Distance / Radius)));
            }
        }
        broadcast(World);
    }

    private static void spawnWave(ServerLevel World, Scp5001State State, Scp5001EventData Data, int Wave) {
        List<ServerPlayer> Inside = playersInside(World, State);
        int Count = Math.min(Scp5001EventConfig.swarmCap(), Scp5001EventConfig.swarmBase() + Scp5001EventConfig.swarmPerPlayer() * Math.max(1, Inside.size()));
        Scp610Form[] Pool = SwarmPools[Math.min(Wave, SwarmPools.length - 1)];
        BlockPos Core = State.core();
        Scp5001Vessel Vessel = vessel(World, State);
        for (int Index = 0; Index < Count; Index++) {
            Scp610Form Form = Pool[World.random.nextInt(Pool.length)];
            float Angle = World.random.nextFloat() * Mth.TWO_PI;
            double Reach = 3.0 + World.random.nextDouble() * 6.0;
            Vec3 At = new Vec3(Core.getX() + 0.5 + Mth.cos(Angle) * Reach, Core.getY(), Core.getZ() + 0.5 + Mth.sin(Angle) * Reach);
            Scp610Creature Risen = Scp610Spawning.raise(World, Form, At, Angle * Mth.RAD_TO_DEG + 90.0f, true);
            if (Risen == null) {
                continue;
            }
            Data.track(Risen.getUUID());
            AttributeInstance Range = Risen.getAttribute(Attributes.FOLLOW_RANGE);
            if (Range != null) {
                Range.setBaseValue(Math.max(Range.getBaseValue(), 96.0));
            }
            if (!Inside.isEmpty()) {
                Risen.setTarget(Inside.get(World.random.nextInt(Inside.size())));
            }
            if (Vessel != null) {
                Vec3 From = Vessel.position();
                for (int Step = 0; Step < 8; Step++) {
                    Vec3 Point = From.lerp(At, Step / 8.0);
                    World.sendParticles(Scp5001Vessel.FleshLight, Point.x, Point.y, Point.z, 2, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
        play(World, Core, Scp5001EventRegistry.Swarm, 6.0f, 0.8f);
    }

    private static void sweep(ServerLevel World, Scp5001EventData Data, long Now) {
        if (Now % SweepEvery != 0) {
            return;
        }
        for (UUID Id : new ArrayList<>(Data.swarm())) {
            Entity Found = World.getEntity(Id);
            if (Found != null && Found.isAlive()) {
                Missing.remove(Id);
                continue;
            }
            int Count = Missing.merge(Id, 1, Integer::sum);
            if (Found != null || Count >= MissingLimit) {
                Missing.remove(Id);
                Data.untrack(Id);
            }
        }
    }

    public static void forgetSwarm(MinecraftServer Server, UUID Id) {
        if (Scp5001EventData.get(Server).untrack(Id)) {
            Missing.remove(Id);
        }
    }

    public static List<ServerPlayer> playersInside(ServerLevel World, Scp5001State State) {
        List<ServerPlayer> Inside = new ArrayList<>();
        for (ServerPlayer Each : World.players()) {
            if (Each.isAlive() && !Each.isSpectator() && Scp5001Exposure.inside(Each, State)) {
                Inside.add(Each);
            }
        }
        return Inside;
    }

    @Nullable
    public static Scp5001Vessel vessel(ServerLevel World, Scp5001State State) {
        if (Cached != null && Cached.isAlive() && Cached.level() == World && Cached.mode() != Scp5001Vessel.ModeBurst) {
            return Cached;
        }
        Cached = null;
        if (!State.located()) {
            return null;
        }
        BlockPos Core = State.core();
        if (!World.isLoaded(Core)) {
            return null;
        }
        AABB Column = new AABB(Core).inflate(8.0, 2.0, 8.0).expandTowards(0.0, Scp5001EventConfig.riseHeight() + Scp5001EventConfig.hoverHeight() + 24.0, 0.0);
        for (Scp5001Vessel Found : World.getEntitiesOfClass(Scp5001Vessel.class, Column, Each -> Each.isAlive() && Each.mode() != Scp5001Vessel.ModeBurst)) {
            Cached = Found;
            return Found;
        }
        return null;
    }

    public static void forgetCache() {
        Cached = null;
        Missing.clear();
    }

    public static List<BlockPos> rods(ServerLevel World, Scp5001State State, Scp5001EventData Data) {
        return Data.devices(Scp5001EventData.Kind.Rod, State.core(), Scp5001EventConfig.siteRadius());
    }

    public static List<BlockPos> lasers(ServerLevel World, Scp5001State State, Scp5001EventData Data) {
        return Data.devices(Scp5001EventData.Kind.Laser, State.core(), Scp5001EventConfig.siteRadius());
    }

    public static List<BlockPos> generators(ServerLevel World, Scp5001State State, Scp5001EventData Data) {
        return Data.devices(Scp5001EventData.Kind.Generator, State.core(), Scp5001EventConfig.siteRadius());
    }

    public static void registerDevice(ServerLevel World, BlockPos Pos, Scp5001EventData.Kind Kind) {
        Scp5001EventData.get(World.getServer()).register(Kind, Pos);
        if (Kind == Scp5001EventData.Kind.Core) {
            Scp5001State State = Scp5001State.get(World.getServer());
            if (!State.located() || !State.core().equals(Pos) && World.isLoaded(State.core()) && !(World.getBlockState(State.core()).getBlock() instanceof Scp5001ChamberCoreBlock)) {
                State.core(Pos);
            }
        }
    }

    public static void syncDevice(ServerLevel World, BlockPos Pos, BlockState Current, Scp5001DeviceBlockEntity Device) {
        if (World != World.getServer().overworld() || Device.kind() == null) {
            return;
        }
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        Data.register(Device.kind(), Pos);
        BlockState Wanted = Current;
        switch (Device.kind()) {
            case Rod -> Wanted = Current.setValue(Scp5001RodEmitterBlock.Spent, Data.rodsSpent());
            case Laser -> Wanted = Current.setValue(Scp5001LaserEmitterBlock.Firing, laserFiring(World, State, Data, Pos));
            case Generator -> {
                boolean Hot = Data.overdriven().contains(Pos) && mine(State.phase()) && State.phase() != Scp5001State.Phase.Dormant;
                Wanted = Current.setValue(Scp5001GeneratorBlock.Overdrive, Hot);
                if (Device instanceof Scp5001GeneratorBlockEntity Generator) {
                    generatorSound(World, Pos, State, Generator, Hot);
                }
            }
            case Core -> {
                Wanted = Current.setValue(Scp5001ChamberCoreBlock.Breached, !mine(State.phase()));
                if (!State.located() || !State.core().equals(Pos) && World.isLoaded(State.core()) && !(World.getBlockState(State.core()).getBlock() instanceof Scp5001ChamberCoreBlock)) {
                    State.core(Pos);
                }
                if (State.core().equals(Pos) && mine(State.phase()) && vessel(World, State) == null) {
                    Scp5001Vessel Made = Scp5001EventRegistry.VesselType.get().create(World);
                    if (Made != null) {
                        Vec3 Spot = Scp5001Vessel.restingSpot(Pos, State.lift());
                        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
                        World.addFreshEntity(Made);
                        Cached = Made;
                    }
                }
            }
        }
        if (Wanted != Current) {
            World.setBlock(Pos, Wanted, Block.UPDATE_CLIENTS);
        }
    }

    private static void generatorSound(ServerLevel World, BlockPos Pos, Scp5001State State, Scp5001GeneratorBlockEntity Generator, boolean Hot) {
        long Now = World.getGameTime();
        if (!Hot) {
            Generator.spinStart(0);
            if (mine(State.phase()) && Now % 80 < Scp5001DeviceBlockEntity.SyncEvery) {
                play(World, Pos, Scp5001EventRegistry.GeneratorHum, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
            }
            return;
        }
        if (Generator.spinStart() == 0) {
            Generator.spinStart(Now);
        }
        long Elapsed = Now - Generator.spinStart();
        if (Elapsed <= Scp5001GeneratorBlockEntity.SpinTicks) {
            float Share = Elapsed / (float) Scp5001GeneratorBlockEntity.SpinTicks;
            play(World, Pos, Scp5001EventRegistry.GeneratorWhine, 1.6f + Share * 1.4f, 0.55f + Share * 0.9f);
            if (Elapsed % 20 < Scp5001DeviceBlockEntity.SyncEvery) {
                play(World, Pos, Scp5001EventRegistry.GeneratorGrind, 1.5f, 0.5f + Share * 0.5f);
            }
            return;
        }
        if (Now % 60 < Scp5001DeviceBlockEntity.SyncEvery) {
            play(World, Pos, Scp5001EventRegistry.GeneratorOverdrive, 2.5f, 0.95f + World.random.nextFloat() * 0.1f);
            play(World, Pos, Scp5001EventRegistry.GeneratorDrone, 2.0f, 0.9f);
        }
        if (World.random.nextInt(3) == 0) {
            play(World, Pos, Scp5001EventRegistry.GeneratorCrackle, 1.2f, 0.8f + World.random.nextFloat() * 0.4f);
        }
    }

    public static boolean laserFiring(ServerLevel World, Scp5001State State, Scp5001EventData Data, BlockPos Pos) {
        Scp5001State.Phase Phase = State.phase();
        List<BlockPos> All = lasers(World, State, Data);
        int Rank = All.indexOf(Pos);
        if (Rank < 0) {
            return false;
        }
        if (Phase == Scp5001State.Phase.Overdrive || Phase == Scp5001State.Phase.Purging) {
            return true;
        }
        if (Phase == Scp5001State.Phase.Resetting) {
            return World.getGameTime() - Data.resetStart() < BlastTicks;
        }
        if (Phase != Scp5001State.Phase.Rising) {
            return false;
        }
        int Generators = Math.max(1, generators(World, State, Data).size());
        int Firing = (int) Math.ceil(State.overdriven() * All.size() / (double) Generators);
        return Rank < Firing;
    }

    public static void openConsole(ServerPlayer User, BlockPos Pos, int Screen) {
        ServerLevel World = User.serverLevel();
        sendState(User);
        BlockPos Linked = Pos;
        int Node = 0;
        if (Screen == Scp5001OpenPayload.GeneratorScreen && World == World.getServer().overworld()) {
            Scp5001State State = Scp5001State.get(World.getServer());
            Scp5001EventData Data = Scp5001EventData.get(World.getServer());
            BlockPos Found = linkedGenerator(Data, Pos);
            if (Found != null) {
                Linked = Found;
                List<BlockPos> All = generators(World, State, Data);
                Node = All.indexOf(Found) + 1;
            }
        }
        PacketDistributor.sendToPlayer(User, new Scp5001OpenPayload(Screen, Pos, Linked, Node));
        World.playSound(null, Pos, Scp5001EventRegistry.ConsoleBeep.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
    }

    @Nullable
    public static BlockPos linkedGenerator(Scp5001EventData Data, BlockPos Console) {
        BlockPos Best = null;
        double Closest = GeneratorReach * GeneratorReach;
        for (BlockPos Each : Data.devices(Scp5001EventData.Kind.Generator, Console, GeneratorReach)) {
            double Distance = Each.distSqr(Console);
            if (Distance <= Closest) {
                Closest = Distance;
                Best = Each;
            }
        }
        return Best;
    }

    public static void action(ServerPlayer User, Scp5001ActionPayload Payload) {
        ServerLevel World = User.serverLevel();
        BlockPos Pos = Payload.Pos();
        if (World != World.getServer().overworld() || User.isSpectator() || User.blockPosition().distSqr(Pos) > ReachSquared || !World.isLoaded(Pos)) {
            return;
        }
        BlockState Block0 = World.getBlockState(Pos);
        boolean Main = Block0.is(Scp5001EventRegistry.ConsoleMain.get());
        boolean Station = Block0.is(Scp5001EventRegistry.GeneratorConsole.get());
        long Now = World.getGameTime();
        boolean Done = switch (Payload.Action()) {
            case Scp5001ActionPayload.CoverAction -> Main && cover(World, Pos);
            case Scp5001ActionPayload.ToggleAction -> Main && unlock(World, Now);
            case Scp5001ActionPayload.OverdriveAction -> {
                BlockPos Generator = Station ? linkedGenerator(Scp5001EventData.get(World.getServer()), Pos) : null;
                yield Generator != null && overdriveGenerator(World, Generator, Now);
            }
            case Scp5001ActionPayload.ResetAction -> Main && forceReset(World, Now);
            default -> false;
        };
        if (!Done && Payload.Action() != Scp5001ActionPayload.CoverAction) {
            World.playSound(null, Pos, Scp5001EventRegistry.ConsoleDeny.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
        }
        sendState(User);
    }

    private static boolean cover(ServerLevel World, BlockPos Pos) {
        World.playSound(null, Pos, Scp5001EventRegistry.Cover.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        return true;
    }

    public static boolean unlock(ServerLevel World, long Now) {
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        if (State.phase() != Scp5001State.Phase.Dormant || Data.cooling(Now) || !State.located()) {
            return false;
        }
        State.lift(0.0f);
        State.overdriven(0);
        Data.clearOverdrive();
        Data.clearSwarm();
        Data.lastSeen(Scp5001State.Phase.Unlocked);
        State.phase(Scp5001State.Phase.Unlocked, Now);
        play(World, State.core(), Scp5001EventRegistry.InterlockFail, 6.0f, 0.8f);
        alarm(World, State, Data);
        broadcast(World);
        return true;
    }

    public static boolean overdriveGenerator(ServerLevel World, BlockPos Generator, long Now) {
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        if (State.phase() != Scp5001State.Phase.Rising || !generators(World, State, Data).contains(Generator) || !Data.overdrive(Generator)) {
            return false;
        }
        State.overdriven(Data.overdriven().size());
        State.lift((float) Math.max(0.0, State.lift() - Scp5001EventConfig.overdriveReverse()));
        play(World, Generator, Scp5001EventRegistry.GeneratorSpinup, 4.0f, 0.9f);
        play(World, Generator, Scp5001EventRegistry.GeneratorGrind, 2.0f, 0.5f);
        for (BlockPos Laser : lasers(World, State, Data)) {
            play(World, Laser, Scp5001EventRegistry.LaserFire, 3.0f, 0.9f + World.random.nextFloat() * 0.2f);
        }
        if (World.isLoaded(Generator)) {
            BlockState Current = World.getBlockState(Generator);
            if (Current.hasProperty(Scp5001GeneratorBlock.Overdrive)) {
                World.setBlock(Generator, Current.setValue(Scp5001GeneratorBlock.Overdrive, true), Block.UPDATE_CLIENTS);
            }
        }
        broadcast(World);
        return true;
    }

    public static boolean forceReset(ServerLevel World, long Now) {
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        if (State.phase() != Scp5001State.Phase.Purging) {
            return false;
        }
        Data.resetStart(Now);
        Data.rodsSpent(true);
        Data.lastSeen(Scp5001State.Phase.Resetting);
        State.phase(Scp5001State.Phase.Resetting, Now);
        Scp5001Vessel Vessel = vessel(World, State);
        Vec3 Centre = Vessel != null ? Vessel.centre() : Vec3.atCenterOf(State.core()).add(0.0, 8.0, 0.0);
        for (BlockPos Rod : rods(World, State, Data)) {
            Vec3 From = Vec3.atCenterOf(Rod);
            for (int Step = 0; Step <= 16; Step++) {
                Vec3 Point = From.lerp(Centre, Step / 16.0 * LatchShare);
                World.sendParticles(ParticleTypes.END_ROD, Point.x, Point.y, Point.z, 3, 0.15, 0.15, 0.15, 0.05);
                World.sendParticles(ParticleTypes.LARGE_SMOKE, Point.x, Point.y, Point.z, 1, 0.1, 0.1, 0.1, 0.01);
            }
            play(World, Rod, Scp5001EventRegistry.RodVaporize, 3.0f, 0.8f + World.random.nextFloat() * 0.3f);
            if (World.isLoaded(Rod)) {
                BlockState Current = World.getBlockState(Rod);
                if (Current.hasProperty(Scp5001RodEmitterBlock.Spent)) {
                    World.setBlock(Rod, Current.setValue(Scp5001RodEmitterBlock.Spent, true), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (Scp5001Mimic Each : World.getEntitiesOfClass(Scp5001Mimic.class, siteBox(State))) {
            World.sendParticles(Scp5001Vessel.FieldLight, Each.getX(), Each.getY() + 1.0, Each.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            Each.discard();
        }
        broadcast(World);
        return true;
    }

    public static AABB siteBox(Scp5001State State) {
        return new AABB(State.core()).inflate(Scp5001EventConfig.siteRadius(), Scp5001EventConfig.siteHeight(), Scp5001EventConfig.siteRadius());
    }

    private static void alarm(ServerLevel World, Scp5001State State, Scp5001EventData Data) {
        play(World, State.core(), Scp5001EventRegistry.Alarm, 8.0f, 1.0f);
        for (ServerPlayer Each : playersInside(World, State)) {
            World.playSound(null, Each.getX(), Each.getY(), Each.getZ(), Scp5001EventRegistry.AlarmTone.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        }
    }

    private static void play(ServerLevel World, BlockPos Pos, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch) {
        World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Sound.get(), SoundSource.BLOCKS, Volume, Pitch);
    }

    public static Scp5001StatePayload snapshot(ServerLevel World) {
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        List<BlockPos> Rods = rods(World, State, Data);
        return new Scp5001StatePayload(State.phase().ordinal(), State.phaseTick(), State.lift(), State.overdriven(), generators(World, State, Data).size(), Rods.size(),
                Data.rodsSpent(), Data.cooldownEnd(), Data.swarmWave(), Scp5001EventConfig.swarmWaves(), Data.swarm().size(), State.core(), State.located(),
                new ArrayList<>(Data.overdriven()), Data.riseStart(), Data.resetStart(), Scp5001EventConfig.riseTicks(), Scp5001EventConfig.rodLatchTicks());
    }

    public static void broadcast(ServerLevel World) {
        if (World.players().isEmpty()) {
            return;
        }
        Scp5001StatePayload Payload = snapshot(World);
        for (ServerPlayer Each : World.players()) {
            PacketDistributor.sendToPlayer(Each, Payload);
        }
    }

    public static void sendState(ServerPlayer User) {
        ServerLevel Overworld = User.getServer().overworld();
        PacketDistributor.sendToPlayer(User, snapshot(Overworld));
    }
}
