package net.rai.scp_unleashed.door.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.door.DoorCue;
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class HeavyDoorSounds {
    public static final int LoopAttack = 3;
    public static final int LoopRelease = 2;
    public static final int GrainAttack = 2;
    public static final int GrainRelease = 6;
    public static final float Jitter = 0.05f;
    public static final double Reach = 48.0;

    private static final Map<PanelDoorBlockEntity, Integer> Clocks = new WeakHashMap<>();

    private HeavyDoorSounds() {
    }

    public static void cue(PanelDoorBlockEntity Door, DoorCue Cue) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Door.getLevel();
        BlockState State = Door.getBlockState();
        if (World == null || Game.level != World || !(State.getBlock() instanceof PanelDoorBlock Frame)) {
            return;
        }
        Optional<HeavyDoorScore> Found = HeavyDoorScore.of(Frame.getKind().getVoice());
        if (Found.isEmpty()) {
            return;
        }
        HeavyDoorScore Score = Found.get();
        Vec3 Centre = DoorGeometry.centre(Frame.getKind(), Door.getBlockPos(), State.getValue(PanelDoorBlock.Facing));
        if (Game.gameRenderer.getMainCamera().getPosition().distanceToSqr(Centre) > Reach * Reach) {
            return;
        }
        boolean Open = State.getValue(PanelDoorBlock.Open);
        RandomSource Random = World.getRandom();
        switch (Cue) {
            case Start -> {
                Clocks.put(Door, 0);
                playAll(Door, Open ? Score.Opening() : Score.Closing(), Centre, Random);
            }
            case Reverse -> playAll(Door, Score.Reversing(), Centre, Random);
            case Travel -> travel(Door, Score, Centre, Random);
            case Arrive -> playAll(Door, Open ? Score.Opened() : Score.Closed(), Centre, Random);
        }
    }

    private static void travel(PanelDoorBlockEntity Door, HeavyDoorScore Score, Vec3 Centre, RandomSource Random) {
        if (!Door.isTravelling()) {
            return;
        }
        int Clock = Clocks.getOrDefault(Door, 0);
        Clocks.put(Door, Clock + 1);
        if (Clock % Score.GrainEvery() == 0) {
            Score.Grain().ifPresent(Part -> play(Door, Part, Centre, Random));
        }
        float Before = Door.getProgress(0.0f);
        float After = Door.getProgress(1.0f);
        for (float Mark : Score.ClankAt()) {
            if (Math.min(Before, After) < Mark && Mark <= Math.max(Before, After)) {
                Score.Clank().ifPresent(Part -> play(Door, Part, Centre, Random));
            }
        }
    }

    private static void playAll(PanelDoorBlockEntity Door, List<DoorPart> Parts, Vec3 Centre, RandomSource Random) {
        for (DoorPart Part : Parts) {
            play(Door, Part, Centre, Random);
        }
    }

    private static void play(PanelDoorBlockEntity Door, DoorPart Part, Vec3 Centre, RandomSource Random) {
        SoundManager Sounds = Minecraft.getInstance().getSoundManager();
        if (!Part.isHit()) {
            Sounds.play(new DoorLayerSound(Door, Part, Centre, Random));
            return;
        }
        float Pitch = Part.Pitch() * (1.0f + Jitter * Random.nextFloat());
        SimpleSoundInstance Hit = new SimpleSoundInstance(Part.Event().get(), SoundSource.BLOCKS, Part.loudnessAt(Door.getSpeed()), Pitch,
                RandomSource.create(Random.nextLong()), Centre.x, Centre.y, Centre.z);
        if (Part.Delay() > 0) {
            Sounds.playDelayed(Hit, Part.Delay() + 1);
        } else {
            Sounds.play(Hit);
        }
    }
}
