package net.rai.scp_unleashed.particleemitter.client;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlock;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ColdFloorEffect {
    public static final double ViewRange = 96.0;
    public static final int CheckTicks = 10;
    public static final int LightTicks = 10;
    public static final int BuildsPerTick = 1;
    public static final float FadeBand = 3.0f;
    public static final float EdgeDensity = 0.15f;
    public static final float FillRate = 0.1f;
    public static final float FillStep = 0.01f;
    public static final float FeedLevel = 0.45f;
    public static final float DrainRate = 0.022f;
    public static final float Visible = 0.004f;
    public static final float Tie = 1.0e-4f;
    public static final float FallbackDrop = 2.0f;
    private static final int[][] CornerSteps = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final int[][] Around = {{-1, -1}, {0, -1}, {-1, 0}, {0, 0}};
    private static final int[][] Neighbours = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}, {0, 1, 0}, {0, -1, 0}};
    private static final List<ColdFloorEffect> Active = new ArrayList<>();
    private static long BuildTick = Long.MIN_VALUE;
    private static int BuildsUsed;

    private final ParticleEmitterBlockEntity Emitter;
    private final long Order;
    private final int CheckOffset;
    private final Long2IntOpenHashMap Index = new Long2IntOpenHashMap();
    @Nullable
    private ColdFloorField Field;
    private boolean Dirty = true;
    private boolean LightStale = true;
    private boolean Showing;
    private ColdFloorField.Cell[] Cells = new ColdFloorField.Cell[0];
    private float[] Alpha = new float[0];
    private float[] OldAlpha = new float[0];
    private int[][] Links = new int[0][];
    private float[] Corners = new float[0];
    private float[] OldCorners = new float[0];
    private float[] Heights = new float[0];
    private boolean[] Owned = new boolean[0];
    private int[] Light = new int[0];
    private int[] Pours = new int[0];
    private float[] PourTops = new float[0];
    private float[] PourBottoms = new float[0];
    private int Stream = -1;
    private float StreamBottom;

    private ColdFloorEffect(ParticleEmitterBlockEntity Emitter) {
        this.Emitter = Emitter;
        this.Order = Emitter.getBlockPos().asLong();
        this.CheckOffset = Math.floorMod(Emitter.getBlockPos().hashCode(), CheckTicks);
        this.Index.defaultReturnValue(-1);
    }

    public static void tick(Minecraft Game, ParticleEmitterBlockEntity Emitter) {
        ColdFloorEffect State = Emitter.getClientState() instanceof ColdFloorEffect Known ? Known : null;
        if (State == null) {
            State = new ColdFloorEffect(Emitter);
            Emitter.setClientState(State);
        }
        State.update(Game);
    }

    @Nullable
    public static ColdFloorEffect of(ParticleEmitterBlockEntity Emitter) {
        return Emitter.getClientState() instanceof ColdFloorEffect Known ? Known : null;
    }

    private void update(Minecraft Game) {
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        long Now = World.getGameTime();
        Active.removeIf(Each -> Each.Emitter.isRemoved() || Each.Emitter.getLevel() != World);
        boolean Powered = Emitter.isEmitting();
        BlockPos Pos = Emitter.getBlockPos();
        Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition();
        boolean Near = Eye.distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5) < ViewRange * ViewRange;
        if (Powered && Near) {
            if (Field != null && !Dirty && (Now + CheckOffset) % CheckTicks == 0 && Field.stale(World)) {
                Dirty = true;
            }
            if ((Field == null || Dirty) && claimBuild(Now)) {
                adopt(ColdFloorField.build(World, Pos));
                Dirty = false;
            }
        }
        flow(Powered);
        Showing = false;
        for (float Each : Alpha) {
            if (Each > Visible) {
                Showing = true;
                break;
            }
        }
        if (!Showing) {
            Active.remove(this);
            System.arraycopy(Corners, 0, OldCorners, 0, Corners.length);
            return;
        }
        if (!Active.contains(this)) {
            Active.add(this);
        }
        if (LightStale || (Now + CheckOffset) % LightTicks == 0) {
            refreshLight(World);
        }
        shade();
    }

    private void adopt(ColdFloorField Made) {
        Long2FloatOpenHashMap Kept = new Long2FloatOpenHashMap();
        for (int Slot = 0; Slot < Cells.length; Slot++) {
            Kept.put(key(Cells[Slot]), Alpha[Slot]);
        }
        Field = Made;
        Cells = Made.cells();
        int Count = Cells.length;
        Index.clear();
        for (int Slot = 0; Slot < Count; Slot++) {
            Index.put(key(Cells[Slot]), Slot);
        }
        Alpha = new float[Count];
        OldAlpha = new float[Count];
        for (int Slot = 0; Slot < Count; Slot++) {
            Alpha[Slot] = Kept.get(key(Cells[Slot]));
            OldAlpha[Slot] = Alpha[Slot];
        }
        Links = new int[Count][];
        IntArrayList Found = new IntArrayList();
        for (int Slot = 0; Slot < Count; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            Found.clear();
            for (int[] Step : Neighbours) {
                int Other = Index.get(BlockPos.asLong(Here.X() + Step[0], Here.Y() + Step[1], Here.Z() + Step[2]));
                if (Other >= 0) {
                    Found.add(Other);
                }
            }
            Links[Slot] = Found.toIntArray();
        }
        Heights = new float[Count * 4];
        for (int Slot = 0; Slot < Count; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            if (!Here.Grounded()) {
                continue;
            }
            for (int Corner = 0; Corner < 4; Corner++) {
                double Top = Here.FloorY();
                int Cx = Here.X() + CornerSteps[Corner][0];
                int Cz = Here.Z() + CornerSteps[Corner][1];
                for (int[] Side : Around) {
                    int Other = Index.get(BlockPos.asLong(Cx + Side[0], Here.Y(), Cz + Side[1]));
                    if (Other >= 0 && Cells[Other].Grounded()) {
                        Top = Math.max(Top, Cells[Other].FloorY());
                    }
                }
                Heights[Slot * 4 + Corner] = (float) (Top - Here.Y());
            }
        }
        Corners = new float[Count * 4];
        OldCorners = new float[Count * 4];
        Owned = new boolean[Count];
        Light = new int[Count];
        LightStale = true;
        IntArrayList Ledges = new IntArrayList();
        for (int Slot = 0; Slot < Count; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            if (!Here.Grounded() && (Here.StepX() != 0 || Here.StepZ() != 0)) {
                Ledges.add(Slot);
            }
        }
        Pours = Ledges.toIntArray();
        PourTops = new float[Pours.length];
        PourBottoms = new float[Pours.length];
        for (int Pour = 0; Pour < Pours.length; Pour++) {
            ColdFloorField.Cell Here = Cells[Pours[Pour]];
            ColdFloorField.Cell Parent = Made.at(Here.X() - Here.StepX(), Here.Y(), Here.Z() - Here.StepZ());
            PourTops[Pour] = (float) ((Parent != null ? Parent.FloorY() : Here.Y()) - Here.Y());
            PourBottoms[Pour] = landingBelow(Made, Here);
        }
        Stream = -1;
        Direction Facing = Emitter.getFacing();
        int Source = Index.get(Emitter.getBlockPos().asLong());
        if (Facing != Direction.UP && Source >= 0 && !Cells[Source].Grounded()) {
            Stream = Source;
            StreamBottom = landingBelow(Made, Cells[Source]);
        }
        shade();
        System.arraycopy(Corners, 0, OldCorners, 0, Corners.length);
    }

    private static float landingBelow(ColdFloorField Made, ColdFloorField.Cell From) {
        for (int Fall = 1; Fall <= ColdFloorField.MaxDrop; Fall++) {
            ColdFloorField.Cell Below = Made.at(From.X(), From.Y() - Fall, From.Z());
            if (Below != null && Below.Grounded()) {
                return (float) (Below.FloorY() - From.Y());
            }
        }
        return -FallbackDrop;
    }

    private static long key(ColdFloorField.Cell Here) {
        return BlockPos.asLong(Here.X(), Here.Y(), Here.Z());
    }

    private static float density(ColdFloorField.Cell Here) {
        return Mth.clamp((ColdFloorField.Radius + 0.5f - Here.Distance()) / FadeBand, EdgeDensity, 1.0f);
    }

    private void flow(boolean Powered) {
        System.arraycopy(Alpha, 0, OldAlpha, 0, Alpha.length);
        for (int Slot = 0; Slot < Cells.length; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            float Goal = Powered ? density(Here) : 0.0f;
            float Have = OldAlpha[Slot];
            if (Have < Goal) {
                boolean Fed = Here.Distance() <= 0.0f;
                for (int Other : Links[Slot]) {
                    if (Fed) {
                        break;
                    }
                    Fed = OldAlpha[Other] >= FeedLevel * Goal;
                }
                if (Fed) {
                    Alpha[Slot] = Math.min(Goal, Have + (Goal - Have) * FillRate + FillStep);
                }
            } else {
                Alpha[Slot] = Math.max(Goal, Have - DrainRate);
            }
        }
    }

    private void refreshLight(Level World) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Slot = 0; Slot < Cells.length; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            Cursor.set(Here.X(), Here.Y(), Here.Z());
            Light[Slot] = World.isLoaded(Cursor) ? LevelRenderer.getLightColor(World, Cursor) : 0;
        }
        LightStale = false;
    }

    private float groundedAlpha(long Key) {
        int Slot = Index.get(Key);
        return Slot >= 0 && Cells[Slot].Grounded() ? Alpha[Slot] : 0.0f;
    }

    private boolean covers(long Key) {
        if (Field == null) {
            return false;
        }
        AABB Box = Field.bounds();
        int X = BlockPos.getX(Key);
        int Y = BlockPos.getY(Key);
        int Z = BlockPos.getZ(Key);
        return X >= Box.minX && X < Box.maxX && Y >= Box.minY && Y < Box.maxY && Z >= Box.minZ && Z < Box.maxZ;
    }

    private float others(long Key) {
        float Best = 0.0f;
        for (ColdFloorEffect Other : Active) {
            if (Other != this && Other.covers(Key)) {
                Best = Math.max(Best, Other.groundedAlpha(Key));
            }
        }
        return Best;
    }

    private boolean owns(long Key, float Mine) {
        for (ColdFloorEffect Other : Active) {
            if (Other == this || !Other.covers(Key)) {
                continue;
            }
            float Theirs = Other.groundedAlpha(Key);
            if (Theirs > Mine + Tie || (Theirs > 0.0f && Math.abs(Theirs - Mine) <= Tie && Other.Order < Order)) {
                return false;
            }
        }
        return true;
    }

    private void shade() {
        System.arraycopy(Corners, 0, OldCorners, 0, Corners.length);
        boolean Shared = Active.size() > 1;
        for (int Slot = 0; Slot < Cells.length; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            if (!Here.Grounded()) {
                continue;
            }
            Owned[Slot] = !Shared || owns(key(Here), Alpha[Slot]);
            for (int Corner = 0; Corner < 4; Corner++) {
                int Cx = Here.X() + CornerSteps[Corner][0];
                int Cz = Here.Z() + CornerSteps[Corner][1];
                float Sum = 0.0f;
                int Count = 0;
                for (int[] Side : Around) {
                    long Key = BlockPos.asLong(Cx + Side[0], Here.Y(), Cz + Side[1]);
                    int Other = Index.get(Key);
                    if (Other >= 0) {
                        Count++;
                        if (Cells[Other].Grounded()) {
                            Sum += Shared ? Math.max(Alpha[Other], others(Key)) : Alpha[Other];
                        }
                        continue;
                    }
                    if (Field != null && Field.rim(Key) == ColdFloorField.Solid) {
                        continue;
                    }
                    Count++;
                    if (Shared) {
                        Sum += others(Key);
                    }
                }
                Corners[Slot * 4 + Corner] = Count == 0 ? 0.0f : Sum / Count;
            }
        }
    }

    public boolean showing() {
        return Showing;
    }

    public ColdFloorField.Cell[] cells() {
        return Cells;
    }

    public boolean owned(int Slot) {
        return Owned[Slot];
    }

    public float alpha(int Slot, float Partial) {
        return Mth.lerp(Partial, OldAlpha[Slot], Alpha[Slot]);
    }

    public float corner(int Slot, int Corner, float Partial) {
        int At = Slot * 4 + Corner;
        return Mth.lerp(Partial, OldCorners[At], Corners[At]);
    }

    public float height(int Slot, int Corner) {
        return Heights[Slot * 4 + Corner];
    }

    public int light(int Slot) {
        return Light[Slot];
    }

    public int[] pours() {
        return Pours;
    }

    public float pourTop(int Pour) {
        return PourTops[Pour];
    }

    public float pourBottom(int Pour) {
        return PourBottoms[Pour];
    }

    public int stream() {
        return Stream;
    }

    public float streamBottom() {
        return StreamBottom;
    }

    public float streamTop() {
        Direction Facing = Emitter.getFacing();
        double Depth = ParticleEmitterBlock.Depth / 16.0;
        return (float) (Facing == Direction.DOWN ? 1.0 - Depth : 0.5);
    }

    @Nullable
    public AABB bounds() {
        return Field == null ? null : Field.bounds();
    }

    private static boolean claimBuild(long Now) {
        if (Now != BuildTick) {
            BuildTick = Now;
            BuildsUsed = 0;
        }
        if (BuildsUsed >= BuildsPerTick) {
            return false;
        }
        BuildsUsed++;
        return true;
    }
}
