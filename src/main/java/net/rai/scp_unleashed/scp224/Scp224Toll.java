package net.rai.scp_unleashed.scp224;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.util.List;

public final class Scp224Toll {
    public static final float HearingRange = 48.0f;
    public static final int MinGap = 6000;
    public static final int MaxGap = 18000;
    public static final int Strokes = 13;
    public static final int StrokeGap = 32;
    public static final int StillMin = 24000;
    public static final int StillMax = 48000;
    public static final float StrokeVolume = 1.0f;
    public static final float[] LayerVolumes = {1.0f, 0.85f, 0.7f};

    private static final Logger Log = LogUtils.getLogger();

    private Scp224Toll() {
    }

    public static long gap(RandomSource Random) {
        return MinGap + (long) Random.nextInt(MaxGap - MinGap + 1);
    }

    public static long stillFor(RandomSource Random) {
        return StillMin + (long) Random.nextInt(StillMax - StillMin + 1);
    }

    public static boolean heard(ServerLevel World, BlockPos Lower) {
        Vec3 Centre = Vec3.atCenterOf(Lower.above());
        for (Player User : World.players()) {
            if (!User.isSpectator() && User.distanceToSqr(Centre) <= (double) HearingRange * HearingRange) {
                return true;
            }
        }
        return false;
    }

    public static List<LivingEntity> begin(ServerLevel World, BlockPos Lower, Direction Facing, Scp224ClockBlockEntity Clock) {
        Scp224Room Room = Clock.room(World);
        List<LivingEntity> Caught = Scp224Sight.victims(World, Lower, Facing, Room);
        for (LivingEntity Target : Caught) {
            Scp224Aging.seize(Target, World, Lower, Clock.getSerial());
        }
        if (!Caught.isEmpty()) {
            Log.info("SCP-224 at {} {} {} tolled: {} caught (room {} cells, {})", Lower.getX(), Lower.getY(), Lower.getZ(), Caught.size(), Room.size(),
                    Room.isOpen() ? "open" : "enclosed");
        }
        strike(World, Lower, Facing, 0);
        World.gameEvent(null, GameEvent.BLOCK_ACTIVATE, Lower.above());
        return Caught;
    }

    public static void strike(ServerLevel World, BlockPos Lower, Direction Facing, int Index) {
        Vec3 At = Scp224Sight.dial(Lower, Facing);
        float Drift = 1.0f - (Index % 4 == 3 ? 0.012f : 0.0f);
        float Pitch = Drift * (0.985f + World.random.nextFloat() * 0.03f);
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Scp224Registry.TollLayers;
        for (int Layer = 0; Layer < Layers.size(); Layer++) {
            World.playSound(null, At.x, At.y, At.z, Layers.get(Layer).get(), SoundSource.BLOCKS, StrokeVolume * LayerVolumes[Layer], Pitch);
        }
    }
}
