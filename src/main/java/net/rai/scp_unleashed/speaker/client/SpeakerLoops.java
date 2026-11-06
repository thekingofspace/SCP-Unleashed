package net.rai.scp_unleashed.speaker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SpeakerLoops {
    public static final double StartMargin = 4.0;
    public static final double StopMargin = 12.0;
    public static final int ScanTicks = 5;
    private static final Map<BlockPos, SpeakerBlockEntity> Wanted = new HashMap<>();
    private static final Map<BlockPos, SpeakerLoopSound> Playing = new HashMap<>();
    private static final RandomSource Picker = RandomSource.create(0L);
    @Nullable
    private static Level Tracked;
    private static int Countdown;

    private SpeakerLoops() {
    }

    public static void update(SpeakerBlockEntity Speaker) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Speaker.getLevel() != Game.level) {
            return;
        }
        follow(Game.level);
        BlockPos Pos = Speaker.getBlockPos().immutable();
        SoundMessages.Request Loop = Speaker.getLooping();
        if (Loop == null) {
            Wanted.remove(Pos);
            halt(Pos);
            return;
        }
        Wanted.put(Pos, Speaker);
        SpeakerLoopSound Current = Playing.get(Pos);
        if (Current != null && isPlaying(Game, Current) && Current.plays(Loop)) {
            Current.adopt(Speaker);
            return;
        }
        halt(Pos);
        start(Game, Speaker, Loop);
    }

    public static void release(SpeakerBlockEntity Speaker) {
        BlockPos Pos = Speaker.getBlockPos();
        if (Wanted.get(Pos) == Speaker) {
            Wanted.remove(Pos);
            halt(Pos);
        }
    }

    public static void tick() {
        Minecraft Game = Minecraft.getInstance();
        follow(Game.level);
        if (Wanted.isEmpty() || --Countdown > 0) {
            return;
        }
        Countdown = ScanTicks;
        Iterator<Map.Entry<BlockPos, SpeakerBlockEntity>> Entries = Wanted.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<BlockPos, SpeakerBlockEntity> Entry = Entries.next();
            SpeakerBlockEntity Speaker = Entry.getValue();
            SoundMessages.Request Loop = Speaker.getLooping();
            if (Speaker.isRemoved() || Speaker.getLevel() != Game.level || Loop == null) {
                Entries.remove();
                halt(Entry.getKey());
                continue;
            }
            SpeakerLoopSound Current = Playing.get(Entry.getKey());
            if (Current != null && !isPlaying(Game, Current)) {
                halt(Entry.getKey());
                Current = null;
            }
            if (Current == null) {
                start(Game, Speaker, Loop);
            }
        }
    }

    public static void clear() {
        for (BlockPos Pos : new ArrayList<>(Playing.keySet())) {
            halt(Pos);
        }
        Wanted.clear();
    }

    static boolean keeps(SpeakerLoopSound Instance) {
        Minecraft Game = Minecraft.getInstance();
        SpeakerBlockEntity Owner = Instance.getOwner();
        return Playing.get(Instance.getPos()) == Instance && Game.level != null && Owner.getLevel() == Game.level && !Owner.isRemoved()
                && Instance.plays(Owner.getLooping()) && near(Game, Instance.getMouth(), Instance.getRange() + StopMargin);
    }

    private static void follow(@Nullable Level World) {
        if (Tracked != World) {
            clear();
            Tracked = World;
        }
    }

    private static void start(Minecraft Game, SpeakerBlockEntity Speaker, SoundMessages.Request Loop) {
        Float Range = rangeOf(Game, Loop);
        if (Range == null || !near(Game, Speaker.mouth(), Range + StartMargin)) {
            return;
        }
        SpeakerLoopSound Instance = new SpeakerLoopSound(Speaker, Loop, Range);
        Playing.put(Instance.getPos(), Instance);
        Game.getSoundManager().play(Instance);
    }

    private static void halt(BlockPos Pos) {
        SpeakerLoopSound Current = Playing.remove(Pos);
        if (Current != null) {
            Current.halt();
            Minecraft.getInstance().getSoundManager().stop(Current);
        }
    }

    private static boolean isPlaying(Minecraft Game, SpeakerLoopSound Instance) {
        return !Instance.isStopped() && Game.getSoundManager().isActive(Instance);
    }

    @Nullable
    private static Float rangeOf(Minecraft Game, SoundMessages.Request Loop) {
        WeighedSoundEvents Events = Game.getSoundManager().getSoundEvent(Loop.Id());
        if (Events == null) {
            return null;
        }
        Sound Variant = Events.getSound(Picker);
        return Math.max(Loop.Volume(), 1.0f) * Variant.getAttenuationDistance();
    }

    private static boolean near(Minecraft Game, Vec3 Mouth, double Distance) {
        return Game.gameRenderer.getMainCamera().getPosition().distanceToSqr(Mouth) <= Distance * Distance;
    }
}
