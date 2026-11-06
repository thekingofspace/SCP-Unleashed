package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class Scp407Ending {
    public static final int BrownTicks = 50;
    public static final int GreyStart = 40;
    public static final int GreyTicks = 50;
    public static final int CrumbleStart = 90;
    public static final int CrumbleTicks = 60;
    public static final int WitherTicks = CrumbleStart + CrumbleTicks + 10;
    public static final int DoomTick = 100;
    public static final int LeftoverPerTick = 48;
    public static final int CrackleGap = 4;

    private Scp407Ending() {
    }

    public static void begin(ServerLevel World, Scp407Session Session) {
        if (Session.State == Scp407Session.Withering) {
            return;
        }
        Session.State = Scp407Session.Withering;
        Session.WitherStart = World.getGameTime();
        Session.Tracks.clear();
        Session.Browned = 0;
        Session.Greyed = 0;
        Session.Crumbled = 0;
        order(Session);
        for (UUID Id : Session.Creatures) {
            Entity Found = World.getEntity(Id);
            if (Found instanceof Scp407Bound Creature) {
                Creature.beginWither();
            }
        }
        if (World.isLoaded(Session.Origin)) {
            World.playSound(null, Session.Origin, Scp407Registry.WitherCrackle.get(), SoundSource.BLOCKS, 1.4f, 0.7f);
        }
    }

    public static void order(Scp407Session Session) {
        Session.Fading.clear();
        Session.Fading.addAll(Session.Grown);
        RandomSource Shuffle = RandomSource.create(Session.Id * 7919L + Session.StartTick);
        for (int Index = Session.Fading.size() - 1; Index > 0; Index--) {
            int Other = Shuffle.nextInt(Index + 1);
            long Swap = Session.Fading.getLong(Index);
            Session.Fading.set(Index, Session.Fading.getLong(Other));
            Session.Fading.set(Other, Swap);
        }
    }

    private static int target(long Elapsed, int Start, int Span, int Count) {
        return (int) Math.ceil(Count * Mth.clamp((Elapsed - Start) / (double) Span, 0.0, 1.0));
    }

    public static boolean tick(ServerLevel World, Scp407Session Session) {
        long Now = World.getGameTime();
        long Elapsed = Now - Session.WitherStart;
        if (Session.Fading.isEmpty() && !Session.Grown.isEmpty()) {
            order(Session);
            Session.Browned = 0;
            Session.Greyed = 0;
            Session.Crumbled = 0;
        }
        LongArrayList Cells = Session.Fading;
        int Count = Cells.size();
        int Brown = Math.min(Count, target(Elapsed, 0, BrownTicks, Count));
        for (; Session.Browned < Brown; Session.Browned++) {
            tint(World, BlockPos.of(Cells.getLong(Session.Browned)), 1);
        }
        int Grey = Math.min(Count, target(Elapsed, GreyStart, GreyTicks, Count));
        for (; Session.Greyed < Grey; Session.Greyed++) {
            tint(World, BlockPos.of(Cells.getLong(Session.Greyed)), 2);
        }
        int Crumble = Math.min(Count, target(Elapsed, CrumbleStart, CrumbleTicks, Count));
        boolean Crackled = false;
        for (; Session.Crumbled < Crumble; Session.Crumbled++) {
            BlockPos Pos = BlockPos.of(Cells.getLong(Session.Crumbled));
            if (!World.isLoaded(Pos)) {
                Session.Leftover.add(Pos.asLong());
                continue;
            }
            if (remove(World, Session, Pos, !Crackled && Now % CrackleGap == 0)) {
                Crackled = true;
            }
        }
        int Budget = LeftoverPerTick;
        for (int Index = Session.Leftover.size() - 1; Index >= 0 && Budget > 0; Index--) {
            BlockPos Pos = BlockPos.of(Session.Leftover.getLong(Index));
            if (World.isLoaded(Pos)) {
                remove(World, Session, Pos, false);
                Session.Leftover.removeLong(Index);
                Budget--;
            }
        }
        Scp407Fusion.holdAll(World, Session);
        if (Session.Act == Scp407Session.ActGenesis && !Session.Struck && Elapsed >= DoomTick) {
            doom(World, Session);
        }
        return Elapsed >= WitherTicks && Session.Crumbled >= Count && Session.Leftover.isEmpty() && (Session.Struck || Session.Act != Scp407Session.ActGenesis);
    }

    private static void tint(ServerLevel World, BlockPos Pos, int Stage) {
        if (!World.isLoaded(Pos)) {
            return;
        }
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof Scp407GrowthBlock && State.getValue(Scp407GrowthBlock.Wither) < Stage) {
            World.setBlock(Pos, State.setValue(Scp407GrowthBlock.Wither, Stage), 2);
        }
    }

    public static boolean remove(ServerLevel World, Scp407Session Session, BlockPos Pos, boolean Loud) {
        long Key = Pos.asLong();
        BlockState State = World.getBlockState(Pos);
        Session.Grown.remove(Key);
        BlockState Back = Session.Replaced.remove(Key);
        if (!(State.getBlock() instanceof Scp407GrowthBlock)) {
            return false;
        }
        Scp407GrowthBlock.crumble(World, Pos, State, Back != null ? Back : Blocks.AIR.defaultBlockState());
        if (Loud) {
            World.playSound(null, Pos, Scp407Registry.WitherCrumble.get(), SoundSource.BLOCKS, 0.7f, 0.85f + World.getRandom().nextFloat() * 0.3f);
        }
        return true;
    }

    public static void doom(ServerLevel World, Scp407Session Session) {
        Session.Struck = true;
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            if (Entry.Struck) {
                continue;
            }
            Entry.Struck = true;
            Entry.Fused = false;
            Scp407Doom.strike(World.getServer(), Entry.Key);
        }
    }

    public static void releaseAll(ServerLevel World, Scp407Session Session) {
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            LivingEntity Target = Scp407Hearing.resolve(World.getServer(), Entry.Key);
            if (Target != null) {
                Scp407Fusion.release(Target);
            }
            Entry.Fused = false;
        }
    }

    public static void clear(ServerLevel World, Scp407Session Session) {
        releaseAll(World, Session);
        for (long Key : Session.Grown.toLongArray()) {
            BlockPos Pos = BlockPos.of(Key);
            if (World.isLoaded(Pos)) {
                remove(World, Session, Pos, false);
            }
        }
        for (UUID Id : Session.Creatures) {
            Entity Found = World.getEntity(Id);
            if (Found instanceof Scp407Bound) {
                Found.discard();
            }
        }
    }
}
