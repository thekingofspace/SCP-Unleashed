package net.rai.scp_unleashed.scp335;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class Scp335Episode {
    public static final Codec<Scp335Episode> EpisodeCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Scp335Phase.PhaseCodec.fieldOf("phase").forGetter(Entry -> Entry.Phase),
            Codec.INT.fieldOf("tick").forGetter(Entry -> Entry.Tick),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(Entry -> Entry.Dimension),
            BlockPos.CODEC.fieldOf("computer").forGetter(Entry -> Entry.Computer),
            Vec3.CODEC.fieldOf("approach").forGetter(Entry -> Entry.Approach),
            Vec3.CODEC.fieldOf("screen").forGetter(Entry -> Entry.Screen),
            Vec3.CODEC.fieldOf("anchor").forGetter(Entry -> Entry.Anchor),
            Codec.INT.listOf().fieldOf("lines").forGetter(Entry -> Entry.Lines),
            Codec.INT.fieldOf("said").forGetter(Entry -> Entry.Said),
            Codec.INT.fieldOf("slot").forGetter(Entry -> Entry.Slot),
            Codec.DOUBLE.fieldOf("best").forGetter(Entry -> Entry.Best),
            Codec.INT.fieldOf("stalled").forGetter(Entry -> Entry.Stalled),
            Codec.BOOL.fieldOf("assisted").forGetter(Entry -> Entry.Assisted),
            Codec.BOOL.fieldOf("lit").forGetter(Entry -> Entry.Lit)
    ).apply(Instance, Scp335Episode::new));

    public Scp335Phase Phase;
    public int Tick;
    public ResourceLocation Dimension;
    public BlockPos Computer;
    public Vec3 Approach;
    public Vec3 Screen;
    public Vec3 Anchor;
    public List<Integer> Lines;
    public int Said;
    public int Slot;
    public double Best;
    public int Stalled;
    public boolean Assisted;
    public boolean Lit;

    public Scp335Episode(Scp335Phase Phase, int Tick, ResourceLocation Dimension, BlockPos Computer, Vec3 Approach, Vec3 Screen, Vec3 Anchor, List<Integer> Lines,
                         int Said, int Slot, double Best, int Stalled, boolean Assisted, boolean Lit) {
        this.Phase = Phase;
        this.Tick = Tick;
        this.Dimension = Dimension;
        this.Computer = Computer.immutable();
        this.Approach = Approach;
        this.Screen = Screen;
        this.Anchor = Anchor;
        this.Lines = new ArrayList<>(Lines);
        this.Said = Said;
        this.Slot = Slot;
        this.Best = Best;
        this.Stalled = Stalled;
        this.Assisted = Assisted;
        this.Lit = Lit;
    }

    public static Scp335Episode blank() {
        return new Scp335Episode(Scp335Phase.Glimpse, 0, Level.OVERWORLD.location(), BlockPos.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, List.of(), 0, 0,
                Double.MAX_VALUE, 0, false, true);
    }

    public void enter(Scp335Phase Next) {
        Phase = Next;
        Tick = 0;
    }
}
