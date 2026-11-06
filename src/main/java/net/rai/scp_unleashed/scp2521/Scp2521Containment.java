package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp2521Containment {
    public static final int Window = 600;
    public static final int Watch = 100;
    private static final Map<Anchor, Long> Armed = new ConcurrentHashMap<>();

    public record Anchor(ResourceKey<Level> Dimension, BlockPos Pos) {
    }

    private Scp2521Containment() {
    }

    public static boolean isDocument(ItemStack Stack) {
        DocumentStamp Stamp = Stack.isEmpty() ? null : Stack.get(ArchiveRegistry.StampComponent.get());
        return Stamp != null && Scp2521Registry.DocumentId.equals(Stamp.Document());
    }

    public static boolean isSand(BlockState State) {
        return State.is(BlockTags.SAND);
    }

    public static void arm(ServerLevel World, BlockPos Pos) {
        Armed.put(new Anchor(World.dimension(), Pos.immutable()), World.getServer().overworld().getGameTime() + Window);
        World.playSound(null, Pos, Scp2521Registry.Anchor.get(), SoundSource.BLOCKS, 0.5f, 0.9f + World.random.nextFloat() * 0.1f);
        World.sendParticles(ParticleTypes.ASH, Pos.getX() + 0.5, Pos.getY() + 1.05, Pos.getZ() + 0.5, 6, 0.25, 0.04, 0.25, 0.0);
    }

    @Nullable
    public static Anchor newest(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        Anchor Best = null;
        long BestUntil = Long.MIN_VALUE;
        for (Map.Entry<Anchor, Long> Entry : new ArrayList<>(Armed.entrySet())) {
            Anchor Found = Entry.getKey();
            ServerLevel World = Server.getLevel(Found.Dimension());
            boolean Valid = Entry.getValue() > Now && World != null && World.isLoaded(Found.Pos()) && isSand(World.getBlockState(Found.Pos()));
            if (!Valid) {
                Armed.remove(Found);
                continue;
            }
            if (Entry.getValue() > BestUntil) {
                Best = Found;
                BestUntil = Entry.getValue();
            }
        }
        return Best;
    }

    public static List<Anchor> armed() {
        return new ArrayList<>(Armed.keySet());
    }

    public static void forgetAll() {
        Armed.clear();
    }

    @Nullable
    public static Scp2521 contain(MinecraftServer Server, Anchor Where, @Nullable ServerPlayer Speaker) {
        ServerLevel World = Server.getLevel(Where.Dimension());
        if (World == null) {
            return null;
        }
        Armed.remove(Where);
        retire(Server, true);
        BlockState Sand = World.getBlockState(Where.Pos());
        if (!isSand(Sand)) {
            return null;
        }
        World.setBlock(Where.Pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Scp2521 Creature = Scp2521Registry.CreatureType.get().create(World);
        if (Creature == null) {
            return null;
        }
        Creature.setRole(Scp2521.RoleContained);
        Creature.setAnchor(Where.Pos());
        Creature.setPos(Where.Pos().getX() + 0.5, Where.Pos().getY(), Where.Pos().getZ() + 0.5);
        if (Speaker != null && Speaker.level() == World) {
            Creature.face(Speaker.position());
        }
        Creature.setSquash(Scp2521Trips.squashAt(World, Where.Pos()));
        Creature.setSand(Block.getId(Sand));
        Creature.setMode(Scp2521.ModeHolding);
        Scp2521State.get(Server).hold(new Scp2521State.Holding(World.dimension(), Where.Pos(), Creature.getUUID(), Creature.getYRot()));
        World.addFreshEntity(Creature);
        Creature.play(Scp2521Registry.RiseLayers, 0.9f, 1.0f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Sand), Where.Pos().getX() + 0.5, Where.Pos().getY() + 0.5, Where.Pos().getZ() + 0.5, 24, 0.3, 0.3, 0.3,
                0.0);
        return Creature;
    }

    public static boolean retire(MinecraftServer Server, boolean Quietly) {
        Scp2521State State = Scp2521State.get(Server);
        Scp2521State.Holding Held = State.held();
        if (Held == null) {
            return false;
        }
        State.hold(null);
        ServerLevel World = Server.getLevel(Held.Dimension());
        if (World != null) {
            Entity Found = World.getEntity(Held.Creature());
            if (Found instanceof Scp2521 Creature) {
                Creature.setMode(Quietly ? Scp2521.ModeSink : Scp2521.ModeBreak);
                if (!Quietly) {
                    Creature.play(Scp2521Registry.BreakLayers, 1.0f, 1.0f);
                }
            }
        }
        return true;
    }

    public static boolean breakFree(MinecraftServer Server) {
        return retire(Server, false);
    }

    public static boolean held(MinecraftServer Server) {
        return Scp2521State.get(Server).held() != null;
    }

    public static void reform(Scp2521 Creature) {
        Creature.setMode(Scp2521.ModeReform);
        Creature.play(Scp2521Registry.RustleLayers, 0.8f, 0.7f);
    }

    public static void tickCreature(ServerLevel World, Scp2521 Creature) {
        Scp2521State.Holding Held = Scp2521State.get(World.getServer()).held();
        boolean Mine = Held != null && Held.Creature().equals(Creature.getUUID());
        int Mode = Creature.mode();
        int Ticks = Creature.modeTicks();
        if (!Mine) {
            if (Mode != Scp2521.ModeBreak && Mode != Scp2521.ModeSink) {
                Creature.setMode(Scp2521.ModeSink);
            } else if (Ticks >= (Mode == Scp2521.ModeBreak ? Scp2521.BreakTicks : Scp2521.SinkTicks)) {
                Creature.discard();
            }
            return;
        }
        BlockPos Anchor = Creature.anchor();
        if (Anchor != null && Mode == Scp2521.ModeContained) {
            Creature.idle().tick(World, Creature, Anchor);
        } else {
            Creature.idle().reset();
            Creature.setGaze(0.0f);
            Creature.setLean(0.0f);
            if (Anchor != null && Creature.position().distanceToSqr(Vec3.atBottomCenterOf(Anchor)) > 0.01) {
                Creature.setPos(Anchor.getX() + 0.5, Anchor.getY(), Anchor.getZ() + 0.5);
            }
        }
        if (Mode == Scp2521.ModeHolding) {
            if (Ticks == Scp2521.AbsorbFrom) {
                Creature.play(Scp2521Registry.AbsorbLayers, 0.9f, 1.0f);
            }
            if (Ticks >= Scp2521.HoldingTicks) {
                Creature.setMode(Scp2521.ModeContained);
                Creature.setSand(0);
            }
        } else if (Mode == Scp2521.ModeReform && Ticks >= Scp2521.ReformTicks) {
            Creature.setMode(Scp2521.ModeContained);
        } else if (Mode != Scp2521.ModeContained && Mode != Scp2521.ModeReform && Mode != Scp2521.ModeHolding) {
            Creature.setMode(Scp2521.ModeContained);
        }
        if (Mode == Scp2521.ModeContained && World.getGameTime() % 160 == Math.floorMod(Creature.getId(), 160)) {
            Creature.play(List.of(Scp2521Registry.RustleWet), 0.25f, 0.7f);
        }
    }

    public static void watch(MinecraftServer Server) {
        if (Server.overworld().getGameTime() % Watch != 0) {
            return;
        }
        Scp2521State State = Scp2521State.get(Server);
        Scp2521State.Holding Held = State.held();
        if (Held == null) {
            return;
        }
        ServerLevel World = Server.getLevel(Held.Dimension());
        if (World == null || !World.isLoaded(Held.Pos()) || !World.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(Held.Pos()))) {
            return;
        }
        if (World.getEntity(Held.Creature()) instanceof Scp2521) {
            return;
        }
        Scp2521 Creature = Scp2521Registry.CreatureType.get().create(World);
        if (Creature == null) {
            return;
        }
        Creature.setRole(Scp2521.RoleContained);
        Creature.setAnchor(Held.Pos());
        Creature.setPos(Held.Pos().getX() + 0.5, Held.Pos().getY(), Held.Pos().getZ() + 0.5);
        Creature.turn(Held.Yaw());
        Creature.setSquash(Scp2521Trips.squashAt(World, Held.Pos()));
        Creature.setMode(Scp2521.ModeReform);
        State.hold(new Scp2521State.Holding(Held.Dimension(), Held.Pos(), Creature.getUUID(), Held.Yaw()));
        World.addFreshEntity(Creature);
    }

    public static boolean wanted(MinecraftServer Server, Scp2521 Creature) {
        Scp2521State.Holding Held = Scp2521State.get(Server).held();
        return Held != null && Held.Creature().equals(Creature.getUUID());
    }
}
