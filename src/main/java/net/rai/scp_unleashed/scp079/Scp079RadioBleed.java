package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.radio.RadioBleed;
import net.rai.scp_unleashed.radio.RadioBleedKind;
import net.rai.scp_unleashed.radio.RadioCarry;
import net.rai.scp_unleashed.radio.RadioNetwork;
import net.rai.scp_unleashed.radio.RadioRegistry;
import net.rai.scp_unleashed.radio.RadioRelay;
import net.rai.scp_unleashed.radio.RadioTowerBlockEntity;
import net.rai.scp_unleashed.radio.RadioTowerData;
import net.rai.scp_unleashed.radio.VoiceConfig;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp079RadioBleed {
    public static final int FirstMin = 600;
    public static final int FirstMax = 1200;
    public static final int IntervalMin = 900;
    public static final int IntervalMax = 1800;
    public static final int RetryTicks = 400;
    public static final float HostileShare = 0.5f;
    public static final float BoredShare = 0.65f;
    public static final int SmugTicks = 50;
    public static final List<RadioBleedKind> Picks = List.of(RadioBleedKind.Corrupt, RadioBleedKind.Corrupt, RadioBleedKind.Corrupt, RadioBleedKind.Glitch,
            RadioBleedKind.Glitch, RadioBleedKind.Glitch, RadioBleedKind.Clank, RadioBleedKind.Pulse, RadioBleedKind.Refrain);
    private static final Map<Scp079BlockEntity, Long> Next = new WeakHashMap<>();

    private Scp079RadioBleed() {
    }

    public record Result(int Channel, RadioBleedKind Kind, List<RadioRelay.Hearer> Reached) {
    }

    public static void tick(ServerLevel World, Scp079BlockEntity Computer, long Now, RandomSource Random) {
        if (!Computer.isUpgraded()) {
            Next.remove(Computer);
            return;
        }
        Long Due = Next.get(Computer);
        if (Due == null) {
            Next.put(Computer, Now + Mth.nextInt(Random, FirstMin, FirstMax));
            return;
        }
        if (Now < Due) {
            return;
        }
        Result Done = attempt(World, Computer, Random, null, null);
        Next.put(Computer, Now + (Done == null ? RetryTicks : interval(Computer.getMood(), Random)));
    }

    public static int interval(Scp079Mood Mood, RandomSource Random) {
        int Base = Mth.nextInt(Random, IntervalMin, IntervalMax);
        return switch (Mood) {
            case Annoyed, Glitching -> Math.round(Base * HostileShare);
            case Idle -> Math.round(Base * BoredShare);
            default -> Base;
        };
    }

    @Nullable
    public static Result attempt(ServerLevel World, Scp079BlockEntity Computer, RandomSource Random, @Nullable RadioBleedKind ForcedKind, @Nullable Integer ForcedChannel) {
        if (!Computer.isUpgraded()) {
            return null;
        }
        List<RadioTowerData.Tower> Towers = connected(World, Computer);
        if (Towers.isEmpty()) {
            return null;
        }
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        List<RadioNetwork.Node> Nodes = RadioTowerData.nodes(World, Limits);
        int Channel = ForcedChannel != null ? RadioCarry.clampChannel(ForcedChannel) : choose(RadioBleed.listenedChannels(World, Towers, Limits, Nodes), Random);
        RadioBleedKind Kind = ForcedKind != null ? ForcedKind : Picks.get(Random.nextInt(Picks.size()));
        List<RadioRelay.Hearer> Reached = RadioBleed.broadcast(World, Towers, Channel, Kind, Limits, Nodes);
        World.playSound(null, Computer.getBlockPos(), RadioRegistry.CorruptBlip.get(), SoundSource.BLOCKS, 0.4f, 0.7f);
        if (!Reached.isEmpty()) {
            Computer.hold(Random.nextBoolean() ? Scp079Mood.Smug : Scp079Mood.Laughing, World.getGameTime(), SmugTicks);
        }
        return new Result(Channel, Kind, Reached);
    }

    private static int choose(Map<Integer, Integer> Heard, RandomSource Random) {
        if (Heard.isEmpty()) {
            return Mth.nextInt(Random, RadioCarry.MinChannel, RadioCarry.MaxChannel);
        }
        int Total = 0;
        for (int Count : Heard.values()) {
            Total += Count;
        }
        int Roll = Random.nextInt(Total);
        for (Map.Entry<Integer, Integer> Entry : Heard.entrySet()) {
            Roll -= Entry.getValue();
            if (Roll < 0) {
                return Entry.getKey();
            }
        }
        return Heard.keySet().iterator().next();
    }

    public static List<RadioTowerData.Tower> connected(ServerLevel World, Scp079BlockEntity Computer) {
        List<RadioTowerData.Tower> Found = new ArrayList<>();
        for (WireDevice Device : Scp079Grid.reach(World, Computer).Devices().values()) {
            if (Device instanceof RadioTowerBlockEntity Tower && !Tower.isRemoved() && Tower.isPowered() && Tower.effectiveMasts() > 0) {
                Found.add(Tower.record());
            }
        }
        BlockPos Origin = Computer.getBlockPos();
        BlockPos Screen = Scp079Block.cellPos(Origin, Computer.facing(), Scp079Block.ScreenPart);
        for (RadioTowerData.Tower Each : RadioTowerData.get(World).towers()) {
            if (!Each.relays() || Found.stream().anyMatch(Known -> Known.Pos().equals(Each.Pos()))) {
                continue;
            }
            if (near(Each.Pos(), Origin) || near(Each.Pos(), Screen)) {
                Found.add(Each);
            }
        }
        return Found;
    }

    private static boolean near(BlockPos Tower, BlockPos Cell) {
        return Math.abs(Tower.getX() - Cell.getX()) <= RadioBleed.Reach && Math.abs(Tower.getY() - Cell.getY()) <= RadioBleed.Reach
                && Math.abs(Tower.getZ() - Cell.getZ()) <= RadioBleed.Reach;
    }

    public static void forget(Scp079BlockEntity Computer) {
        Next.remove(Computer);
    }
}
