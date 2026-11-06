package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;

public final class DaeviteRuinsTraps {
    public static final int DartRest = 30;
    public static final int CollapseDelay = 12;

    private DaeviteRuinsTraps() {
    }

    public static void tick(ServerLevel World, DaeviteRuin Ruin, long Now) {
        boolean Changed = false;
        for (DaeviteRuin.Trap Entry : Ruin.Traps) {
            if (Entry.Sprung || Now < Entry.Ready) {
                continue;
            }
            if (Entry.Kind == DaeviteRuin.Dart) {
                if (triggered(World, Entry)) {
                    int Delay = 1;
                    for (BlockPos Pos : Entry.Effects) {
                        if (World.isLoaded(Pos) && World.getBlockState(Pos).getBlock() instanceof DispenserBlock Launcher) {
                            World.scheduleTick(Pos, Launcher, Delay);
                            Delay += 2;
                        }
                    }
                    Entry.Ready = Now + DartRest;
                    Changed = true;
                }
                continue;
            }
            if (Entry.Pending > 0) {
                if (Now >= Entry.Pending) {
                    collapse(World, Entry);
                    Entry.Sprung = true;
                    Changed = true;
                } else if ((Now & 3) == 0) {
                    dust(World, Entry, 3);
                }
                continue;
            }
            if (trodden(World, Entry)) {
                Entry.Pending = Now + CollapseDelay;
                BlockPos Middle = Entry.Triggers.get(Entry.Triggers.size() / 2);
                World.playSound(null, Middle, DaeviteRuinsRegistry.RuinRumble.get(), SoundSource.BLOCKS, 1.2f, 0.8f + World.getRandom().nextFloat() * 0.2f);
                dust(World, Entry, 8);
                Changed = true;
            }
        }
        if (Changed) {
            DaeviteRuinsWorld.get(World.getServer()).setDirty();
        }
    }

    private static boolean triggered(ServerLevel World, DaeviteRuin.Trap Entry) {
        for (BlockPos Pos : Entry.Triggers) {
            if (!World.isLoaded(Pos)) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED)) {
                return true;
            }
            if (World.getBlockEntity(Pos) instanceof ChestBlockEntity && ChestBlockEntity.getOpenCount(World, Pos) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean trodden(ServerLevel World, DaeviteRuin.Trap Entry) {
        if (Entry.Triggers.isEmpty()) {
            return false;
        }
        BlockPos First = Entry.Triggers.get(0);
        AABB Span = new AABB(First);
        for (BlockPos Pos : Entry.Triggers) {
            Span = Span.minmax(new AABB(Pos));
        }
        AABB Above = new AABB(Span.minX, Span.maxY, Span.minZ, Span.maxX, Span.maxY + 1.0, Span.maxZ);
        for (Player User : World.getEntitiesOfClass(Player.class, Above.inflate(0.1), Found -> !Found.isSpectator() && !Found.getAbilities().flying)) {
            for (BlockPos Pos : Entry.Triggers) {
                AABB Cell = new AABB(Pos.getX(), Pos.getY() + 1.0, Pos.getZ(), Pos.getX() + 1.0, Pos.getY() + 1.6, Pos.getZ() + 1.0);
                if (User.getBoundingBox().intersects(Cell)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void dust(ServerLevel World, DaeviteRuin.Trap Entry, int Count) {
        for (BlockPos Pos : Entry.Triggers) {
            BlockState State = World.getBlockState(Pos);
            if (State.isAir()) {
                continue;
            }
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, Count, 0.4, 0.05, 0.4, 0.05);
        }
    }

    private static void collapse(ServerLevel World, DaeviteRuin.Trap Entry) {
        for (BlockPos Pos : Entry.Effects) {
            if (!World.isLoaded(Pos)) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || State.getDestroySpeed(World, Pos) < 0 || World.getBlockEntity(Pos) != null) {
                continue;
            }
            FallingBlockEntity.fall(World, Pos, State);
        }
        if (!Entry.Effects.isEmpty()) {
            BlockPos Middle = Entry.Effects.get(Entry.Effects.size() / 2);
            World.playSound(null, Middle, DaeviteRuinsRegistry.RuinRumble.get(), SoundSource.BLOCKS, 1.6f, 0.55f);
            World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, Middle.getX() + 0.5, Middle.getY() + 0.5, Middle.getZ() + 0.5, 6, 0.8, 0.2, 0.8, 0.01);
        }
    }
}
