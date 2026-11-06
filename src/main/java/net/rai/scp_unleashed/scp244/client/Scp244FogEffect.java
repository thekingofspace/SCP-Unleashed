package net.rai.scp_unleashed.scp244.client;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp244.Scp244Cloud;
import net.rai.scp_unleashed.scp244.Scp244Field;
import net.rai.scp_unleashed.scp244.Scp244Flood;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp244FogEffect {
    public static final int RebuildTicks = 40;
    public static final int FirstBudget = 2500;
    public static final int Budget = 1000;
    public static final int LightTicks = 10;
    public static final int LightBoost = 2;
    public static final float Ease = 0.12f;
    public static final float Step = 0.004f;
    public static final float Visible = 0.01f;
    public static final double ViewRange = 112.0;
    private static final int[][] CornerSteps = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final int[][] Around = {{-1, -1}, {0, -1}, {-1, 0}, {0, 0}};
    static final List<Scp244FogEffect> Active = new ArrayList<>();

    private final Scp244VaseBlockEntity Vase;
    private final int CheckOffset;
    private final Long2IntOpenHashMap Index = new Long2IntOpenHashMap();
    @Nullable
    private Scp244Field Field;
    @Nullable
    private Scp244Flood Flood;
    private long Built = Long.MIN_VALUE;
    private boolean Showing;
    private boolean LightStale = true;
    private long[] Keys = new long[0];
    private float[] Distance = new float[0];
    private float[] Alpha = new float[0];
    private float[] OldAlpha = new float[0];
    private boolean[] Orphan = new boolean[0];
    private boolean[] Floor = new boolean[0];
    private int[] Light = new int[0];
    private int[] Floors = new int[0];
    private float[] Corners = new float[0];
    private float[] OldCorners = new float[0];
    @Nullable
    private AABB Bounds;
    private int VentTimer;
    private boolean Burst;

    private Scp244FogEffect(Scp244VaseBlockEntity Vase) {
        this.Vase = Vase;
        this.CheckOffset = Math.floorMod(Vase.getBlockPos().hashCode(), LightTicks);
        this.Index.defaultReturnValue(-1);
    }

    public static Scp244FogEffect of(Scp244VaseBlockEntity Vase) {
        if (Vase.getClientState() instanceof Scp244FogEffect Known) {
            return Known;
        }
        Scp244FogEffect Made = new Scp244FogEffect(Vase);
        Vase.setClientState(Made);
        return Made;
    }

    @Nullable
    public static Scp244FogEffect peek(Scp244VaseBlockEntity Vase) {
        return Vase.getClientState() instanceof Scp244FogEffect Known ? Known : null;
    }

    public static List<Scp244FogEffect> active() {
        return Active;
    }

    public static void prune(@Nullable Level World) {
        Active.removeIf(Each -> Each.Vase.isRemoved() || Each.Vase.getLevel() != World);
    }

    public Scp244VaseBlockEntity vase() {
        return Vase;
    }

    public void update(Minecraft Game) {
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        long Now = World.getGameTime();
        boolean Wanted = Vase.isOpen() || Vase.intensity() > 0.0f;
        BlockPos Pos = Vase.getBlockPos();
        boolean Near = Game.gameRenderer.getMainCamera().getPosition().distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5) < ViewRange * ViewRange;
        if (Wanted && Near) {
            if (Flood == null && (Field == null || Now - Built >= RebuildTicks)) {
                Flood = new Scp244Flood(World, Pos);
            }
            if (Flood != null && Flood.step(Field == null ? FirstBudget : Budget)) {
                adopt(World, Flood.result());
                Built = Now;
                Flood = null;
            }
        } else {
            Flood = null;
        }
        flow();
        Showing = false;
        for (float Each : Alpha) {
            if (Each > Visible) {
                Showing = true;
                break;
            }
        }
        if (!Showing) {
            Active.remove(this);
            if (!Wanted && Keys.length > 0) {
                clear();
            }
            return;
        }
        if (!Active.contains(this)) {
            Active.add(this);
        }
        refreshLight(World, LightStale ? -1 : (int) Math.floorMod(Now + CheckOffset, (long) LightTicks));
        shade();
    }

    private void clear() {
        Field = null;
        Flood = null;
        Keys = new long[0];
        Distance = new float[0];
        Alpha = new float[0];
        OldAlpha = new float[0];
        Orphan = new boolean[0];
        Floor = new boolean[0];
        Light = new int[0];
        Floors = new int[0];
        Corners = new float[0];
        OldCorners = new float[0];
        Index.clear();
        Bounds = null;
    }

    private void adopt(Level World, Scp244Field Made) {
        LongArrayList NewKeys = new LongArrayList(Made.size());
        FloatArrayList NewDistance = new FloatArrayList(Made.size());
        FloatArrayList NewAlpha = new FloatArrayList(Made.size());
        List<Boolean> NewOrphan = new ArrayList<>();
        for (int Slot = 0; Slot < Made.size(); Slot++) {
            long Key = Made.key(Slot);
            int Old = Index.get(Key);
            NewKeys.add(Key);
            NewDistance.add(Made.distance(Slot));
            NewAlpha.add(Old >= 0 ? Alpha[Old] : 0.0f);
            NewOrphan.add(false);
        }
        for (int Slot = 0; Slot < Keys.length; Slot++) {
            if (Alpha[Slot] > Visible && !Made.contains(BlockPos.of(Keys[Slot]))) {
                NewKeys.add(Keys[Slot]);
                NewDistance.add(Distance[Slot]);
                NewAlpha.add(Alpha[Slot]);
                NewOrphan.add(true);
            }
        }
        Field = Made;
        int Count = NewKeys.size();
        Keys = NewKeys.toLongArray();
        Distance = NewDistance.toFloatArray();
        Alpha = NewAlpha.toFloatArray();
        OldAlpha = Alpha.clone();
        Orphan = new boolean[Count];
        Index.clear();
        for (int Slot = 0; Slot < Count; Slot++) {
            Orphan[Slot] = NewOrphan.get(Slot);
            Index.put(Keys[Slot], Slot);
        }
        Floor = new boolean[Count];
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        List<Integer> FloorSlots = new ArrayList<>();
        double MinX = Double.MAX_VALUE;
        double MinY = Double.MAX_VALUE;
        double MinZ = Double.MAX_VALUE;
        double MaxX = -Double.MAX_VALUE;
        double MaxY = -Double.MAX_VALUE;
        double MaxZ = -Double.MAX_VALUE;
        for (int Slot = 0; Slot < Count; Slot++) {
            long Key = Keys[Slot];
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            MinX = Math.min(MinX, X);
            MinY = Math.min(MinY, Y);
            MinZ = Math.min(MinZ, Z);
            MaxX = Math.max(MaxX, X + 1);
            MaxY = Math.max(MaxY, Y + 1);
            MaxZ = Math.max(MaxZ, Z + 1);
            long Under = BlockPos.asLong(X, Y - 1, Z);
            if (Index.get(Under) >= 0 || Under == Vase.getBlockPos().asLong()) {
                continue;
            }
            Cursor.set(X, Y - 1, Z);
            if (World.isLoaded(Cursor) && !World.getBlockState(Cursor).getCollisionShape(World, Cursor).isEmpty()) {
                Floor[Slot] = true;
                FloorSlots.add(Slot);
            }
        }
        Floors = FloorSlots.stream().mapToInt(Integer::intValue).toArray();
        Corners = new float[Floors.length * 4];
        OldCorners = new float[Floors.length * 4];
        Light = new int[Count];
        LightStale = true;
        Bounds = Count == 0 ? null : new AABB(MinX - 1, MinY - 1, MinZ - 1, MaxX + 1, MaxY + 1, MaxZ + 1);
        shade();
        System.arraycopy(Corners, 0, OldCorners, 0, Corners.length);
    }

    private void flow() {
        System.arraycopy(Alpha, 0, OldAlpha, 0, Alpha.length);
        float Front = Vase.front();
        float Strength = Vase.intensity();
        for (int Slot = 0; Slot < Alpha.length; Slot++) {
            float Goal = Orphan[Slot] ? 0.0f : Scp244Cloud.density(Distance[Slot], Front, Strength);
            float Have = Alpha[Slot];
            float Gap = Goal - Have;
            float Move = Gap * Ease + Math.copySign(Step, Gap);
            Alpha[Slot] = Math.abs(Move) >= Math.abs(Gap) ? Goal : Have + Move;
        }
    }

    private void refreshLight(Level World, int Share) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Slot = Math.max(0, Share); Slot < Keys.length; Slot += Share < 0 ? 1 : LightTicks) {
            Cursor.set(Keys[Slot]);
            int Packed = World.isLoaded(Cursor) ? LevelRenderer.getLightColor(World, Cursor) : 0;
            Light[Slot] = LightTexture.pack(Math.min(15, LightTexture.block(Packed) + LightBoost), LightTexture.sky(Packed));
        }
        LightStale = false;
    }

    private float floorAlpha(int X, int Y, int Z) {
        int Slot = Index.get(BlockPos.asLong(X, Y, Z));
        return Slot >= 0 && Floor[Slot] ? Alpha[Slot] : 0.0f;
    }

    private void shade() {
        System.arraycopy(Corners, 0, OldCorners, 0, Corners.length);
        for (int Entry = 0; Entry < Floors.length; Entry++) {
            long Key = Keys[Floors[Entry]];
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            for (int Corner = 0; Corner < 4; Corner++) {
                int Cx = X + CornerSteps[Corner][0];
                int Cz = Z + CornerSteps[Corner][1];
                float Sum = 0.0f;
                for (int[] Side : Around) {
                    Sum += floorAlpha(Cx + Side[0], Y, Cz + Side[1]);
                }
                Corners[Entry * 4 + Corner] = Sum * 0.25f;
            }
        }
    }

    public boolean showing() {
        return Showing;
    }

    public int count() {
        return Keys.length;
    }

    public long key(int Slot) {
        return Keys[Slot];
    }

    public float alpha(int Slot, float Partial) {
        return OldAlpha[Slot] + (Alpha[Slot] - OldAlpha[Slot]) * Partial;
    }

    public int light(int Slot) {
        return Light[Slot];
    }

    public int[] floors() {
        return Floors;
    }

    public float corner(int Entry, int Corner, float Partial) {
        int At = Entry * 4 + Corner;
        return OldCorners[At] + (Corners[At] - OldCorners[At]) * Partial;
    }

    public float densityAt(BlockPos Pos) {
        int Slot = Index.get(Pos.asLong());
        return Slot < 0 ? 0.0f : Alpha[Slot];
    }

    @Nullable
    public AABB bounds() {
        return Bounds;
    }

    public boolean ventDue() {
        if (VentTimer > 0) {
            VentTimer--;
            return false;
        }
        return true;
    }

    public void ventIn(int Ticks) {
        VentTimer = Ticks;
    }

    public boolean takeBurst(boolean Opening) {
        if (!Opening) {
            Burst = false;
            return false;
        }
        if (Burst) {
            return false;
        }
        Burst = true;
        return true;
    }
}
