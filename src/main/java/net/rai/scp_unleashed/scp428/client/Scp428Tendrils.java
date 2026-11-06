package net.rai.scp_unleashed.scp428.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp428.Scp428Cords;
import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import net.rai.scp_unleashed.scp428.Scp428Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Map;

public final class Scp428Tendrils {
    public static final ResourceLocation Texture = Scp428Registry.location("textures/entity/scp428/tendril.png");
    public static final String HeadBone = "head";
    public static final int Segments = 20;
    public static final double CordWidth = 1.0 / 16.0;
    public static final double KnotWidth = 0.09;
    public static final double KnotStart = 0.95;
    public static final double ReachBase = 0.13;
    public static final double ReachTip = 0.05;
    public static final double GripBase = 0.09;
    public static final double GripTip = 0.05;
    public static final int GripGrowTicks = 4;
    public static final int TouchTicks = 12;
    public static final double TouchWidth = 0.055;
    public static final double MinPixels = 2.0;
    public static final double VPerBlock = 0.5;
    public static final int WantTicks = 40;
    public static final int FreshTicks = 2;
    private static final Map<Integer, Capture> Heads = new HashMap<>();
    private static final Map<Integer, Long> Wanted = new HashMap<>();

    private Scp428Tendrils() {
    }

    public static Vec3 lerped(Entity Owner, float Partial) {
        return new Vec3(Mth.lerp(Partial, Owner.xOld, Owner.getX()), Mth.lerp(Partial, Owner.yOld, Owner.getY()), Mth.lerp(Partial, Owner.zOld, Owner.getZ()));
    }

    public static void clear() {
        Heads.clear();
        Wanted.clear();
    }

    public static void track(@Nullable GeoBone Head) {
        if (Head != null && !Head.isTrackingMatrices()) {
            Head.setTrackingMatrices(true);
        }
    }

    public static void capture(Entity Owner, BakedGeoModel Model) {
        if (Owner instanceof Scp428Instance || Wanted.isEmpty()) {
            return;
        }
        Long Until = Wanted.get(Owner.getId());
        long Now = Owner.level().getGameTime();
        if (Until == null || Until < Now) {
            return;
        }
        Model.getBone(HeadBone).ifPresent(Bone -> {
            if (Bone.isTrackingMatrices()) {
                Heads.put(Owner.getId(), new Capture(new Matrix4f(Bone.getLocalSpaceMatrix()), Now));
            } else {
                Bone.setTrackingMatrices(true);
            }
        });
    }

    @Nullable
    private static Matrix4f captured(LivingEntity Body) {
        long Now = Body.level().getGameTime();
        Wanted.put(Body.getId(), Now + WantTicks);
        if (Wanted.size() > 32) {
            Wanted.values().removeIf(Until -> Until < Now);
            Heads.keySet().removeIf(Id -> !Wanted.containsKey(Id));
        }
        Capture Found = Heads.get(Body.getId());
        return Found != null && Now - Found.At() <= FreshTicks ? Found.Local() : null;
    }

    public static Vec3 headAnchor(LivingEntity Body, @Nullable Matrix4f Local, float Partial) {
        return headAnchor(Body, Local, Partial, 0.0);
    }

    public static Vec3 headAnchor(LivingEntity Body, @Nullable Matrix4f Local, float Partial, double Depth) {
        Vec3 Origin = lerped(Body, Partial);
        if (Local != null) {
            Vector3f Point = Local.transformPosition(new Vector3f(0.0f, (float) Scp428Cords.HeadUp, (float) (Scp428Cords.HeadBack - Depth)));
            return Origin.add(Point.x, Point.y, Point.z);
        }
        if (!Scp428Cords.upright(Body)) {
            return Body.getEyePosition(Partial);
        }
        float Yaw = Mth.rotLerp(Partial, Body.yHeadRotO, Body.yHeadRot);
        float Pitch = Mth.lerp(Partial, Body.xRotO, Body.getXRot());
        return Scp428Cords.headPoint(Origin.add(0.0, Scp428Cords.neckHeight(Body), 0.0), Yaw, Pitch, Body.getScale());
    }

    public static Vec3 backOf(LivingEntity Body, @Nullable Matrix4f Local, float Partial) {
        if (Local != null) {
            Vector3f Behind = Local.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f));
            Vec3 Flat = new Vec3(Behind.x, 0.0, Behind.z);
            if (Flat.lengthSqr() > 1.0e-4) {
                return Flat.normalize();
            }
        }
        return Scp428Cords.forward(Mth.rotLerp(Partial, Body.yHeadRotO, Body.yHeadRot)).reverse();
    }

    public static Vec3 deviceAnchor(Level World, BlockPos Pos) {
        VoxelShape Shape = World.getBlockState(Pos).getShape(World, Pos);
        if (Shape.isEmpty()) {
            return Vec3.atCenterOf(Pos);
        }
        AABB Box = Shape.bounds();
        return new Vec3(Pos.getX() + (Box.minX + Box.maxX) * 0.5, Pos.getY() + (Box.minY + Box.maxY) * 0.5, Pos.getZ() + (Box.minZ + Box.maxZ) * 0.5);
    }

    public static double tetherWidth(double Along, double Rise) {
        double Base = CordWidth + (KnotWidth - CordWidth) * Scp428Cords.smooth((Along - KnotStart) / (1.0 - KnotStart));
        double Fresh = ReachBase * (1.0 - Along) + ReachTip * Along;
        return Base + (Math.max(Base, Fresh) - Base) * (1.0 - Scp428Cords.smooth(Rise));
    }

    public static void tether(Scp428Instance Body, @Nullable GeoBone Head, float Partial, PoseStack Transforms, MultiBufferSource Buffers, int PackedLight) {
        Level World = Body.level();
        if (!(World.getEntity(Body.ownerId()) instanceof Scp428Core Core) || Core.isRemoved()) {
            return;
        }
        Matrix4f Local = Head != null && Head.isTrackingMatrices() ? Head.getLocalSpaceMatrix() : null;
        Vec3 Origin = lerped(Body, Partial);
        Vec3 Anchor = headAnchor(Body, Local, Partial);
        Vec3 Tip = headAnchor(Body, Local, Partial, Scp428Cords.Sink);
        Core.noteAnchor(Body.getId(), Anchor);
        if (Body.isDeadOrDying() || Core.isDeadOrDying()) {
            return;
        }
        Vec3 Centre = lerped(Core, Partial);
        Vec3 From = Scp428Cords.root(Centre, Core.renderWidth(Partial), Core.renderTall(Partial), Anchor);
        Vec3 Back = backOf(Body, Local, Partial);
        Minecraft Game = Minecraft.getInstance();
        Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition().subtract(Origin);
        double PixelSpan = pixelSpan(Game);
        double Time = World.getGameTime() + Partial;
        double Phase = Scp428Cords.phaseOf(Body.getId());
        double Rise = Body.riseProgress(Partial);
        Vec3[] Points = new Vec3[Segments + 2];
        double[] Widths = new double[Segments + 2];
        for (int Index = 0; Index <= Segments; Index++) {
            double Along = Scp428Cords.sample(Index, Segments);
            Points[Index] = Scp428Cords.tetherPoint(From, Anchor, Back, Along, Time, Phase).subtract(Origin);
            Widths[Index] = visible(tetherWidth(Along, Rise), Points[Index], Eye, PixelSpan);
        }
        Points[Segments + 1] = Tip.subtract(Origin);
        Widths[Segments + 1] = Widths[Segments];
        draw(Buffers, Transforms, Points, Widths, light(World, From.add(Anchor).scale(0.5), PackedLight));
    }

    public static Vec3 victimHead(LivingEntity Victim, float Partial) {
        return headAnchor(Victim, captured(Victim), Partial);
    }

    public static void render(Scp428Core Core, float Partial, PoseStack Transforms, MultiBufferSource Buffers, int PackedLight) {
        if (Core.isDeadOrDying()) {
            return;
        }
        Level World = Core.level();
        Vec3 Base = lerped(Core, Partial);
        Minecraft Game = Minecraft.getInstance();
        Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition().subtract(Base);
        double PixelSpan = pixelSpan(Game);
        double Time = World.getGameTime() + Partial;
        double Width = Core.renderWidth(Partial);
        double Height = Core.renderTall(Partial);
        for (Scp428Core.Retract Entry : Core.retracts()) {
            double Age = (World.getGameTime() - Entry.Start() + Partial) / Scp428Core.Retract.Ticks;
            if (Age < 0.0 || Age >= 1.0) {
                continue;
            }
            Vec3 From = Scp428Cords.root(Base, Width, Height, Entry.From());
            double Reach = 1.0 - Scp428Cords.smooth(Age);
            Vec3[] Sides = Scp428Cords.frame(From, Entry.From());
            Vec3[] Points = new Vec3[Segments + 1];
            double[] Widths = new double[Segments + 1];
            double Phase = Entry.Start() * 0.37;
            for (int Index = 0; Index <= Segments; Index++) {
                double Along = (double) Index / Segments * Reach;
                Vec3 Whip = Sides[1].scale(0.4 * Math.sin(Along * Math.PI * 3.0 + Age * 9.0) * Along * Age);
                Points[Index] = Scp428Cords.tetherPoint(From, Entry.From(), Sides[0], Along, Time, Phase).add(Whip).subtract(Base);
                Widths[Index] = visible(CordWidth, Points[Index], Eye, PixelSpan);
            }
            draw(Buffers, Transforms, Points, Widths, light(World, From, PackedLight));
        }
        CompoundTag Grip = Core.gripInfo();
        if (!Grip.isEmpty() && World.getEntity(Grip.getInt("V")) instanceof LivingEntity Victim && Victim.isAlive()) {
            double Elapsed = World.getGameTime() + Partial - Grip.getLong("S");
            double Progress = Mth.clamp(Elapsed / Math.max(1, Grip.getInt("L")), 0.0, 1.0);
            Vec3 VictimBase = lerped(Victim, Partial);
            int Light = light(World, VictimBase.add(0.0, 1.0, 0.0), PackedLight);
            double Grow = Scp428Cords.smooth(Elapsed / GripGrowTicks);
            for (int Arm = 0; Arm < 3; Arm++) {
                Vec3 End = Scp428Cords.gripEnd(Base, VictimBase, Victim.getBbHeight(), Arm);
                Vec3 From = Scp428Cords.gripRoot(Base, VictimBase, Width, Height, Arm);
                Vec3[] Points = new Vec3[Segments + 1];
                double[] Widths = new double[Segments + 1];
                for (int Index = 0; Index <= Segments; Index++) {
                    double Along = (double) Index / Segments * Grow;
                    Points[Index] = Scp428Cords.gripPoint(From, End, Along, Time, Arm * Scp428Cords.GripPhase).subtract(Base);
                    Widths[Index] = visible(GripBase * (1.0 - Along) + GripTip * Along, Points[Index], Eye, PixelSpan);
                }
                draw(Buffers, Transforms, Points, Widths, Light);
            }
            Vec3 HeadForward = Scp428Cords.forward(Mth.rotLerp(Partial, Victim.yHeadRotO, Victim.yHeadRot));
            Vec3 End = Scp428Cords.reachEnd(victimHead(Victim, Partial), HeadForward, Progress);
            Vec3 From = Scp428Cords.reachRoot(Base, Height);
            double Extension = Scp428Cords.reachExtension(Progress);
            if (Extension > 0.01) {
                Vec3[] Points = new Vec3[Segments + 1];
                double[] Widths = new double[Segments + 1];
                for (int Index = 0; Index <= Segments; Index++) {
                    double Share = (double) Index / Segments;
                    Points[Index] = Scp428Cords.reachPoint(From, End, Share * Extension, Time, 1.3).subtract(Base);
                    Widths[Index] = visible(ReachBase * (1.0 - Share) + ReachTip * Share, Points[Index], Eye, PixelSpan);
                }
                draw(Buffers, Transforms, Points, Widths, Light);
            }
        }
        CompoundTag Touch = Core.touchInfo();
        if (!Touch.isEmpty()) {
            double Age = (World.getGameTime() + Partial - Touch.getLong("T")) / TouchTicks;
            if (Age >= 0.0 && Age < 1.0) {
                Vec3 Device = deviceAnchor(World, BlockPos.of(Touch.getLong("P")));
                Vec3 From = Scp428Cords.root(Base, Width, Height, Device);
                double Reach = Math.sin(Math.PI * Age);
                Vec3[] Points = new Vec3[Segments + 1];
                double[] Widths = new double[Segments + 1];
                for (int Index = 0; Index <= Segments; Index++) {
                    double Along = (double) Index / Segments * Reach;
                    Points[Index] = Scp428Cords.reachPoint(From, Device, Along, Time, 0.7).subtract(Base);
                    Widths[Index] = visible(TouchWidth, Points[Index], Eye, PixelSpan);
                }
                draw(Buffers, Transforms, Points, Widths, light(World, Device, PackedLight));
            }
        }
    }

    public static double pixelSpan(Minecraft Game) {
        return 2.0 * Math.tan(Math.toRadians(Game.options.fov().get()) * 0.5) / Math.max(1, Game.getWindow().getHeight());
    }

    public static double visible(double Width, Vec3 Point, Vec3 Eye, double PixelSpan) {
        return Math.max(Width, Point.distanceTo(Eye) * PixelSpan * MinPixels);
    }

    public static int light(Level World, Vec3 Where, int PackedLight) {
        int Here = LevelRenderer.getLightColor(World, BlockPos.containing(Where));
        return LightTexture.pack(Math.max(LightTexture.block(Here), LightTexture.block(PackedLight)), Math.max(LightTexture.sky(Here), LightTexture.sky(PackedLight)));
    }

    public static void draw(MultiBufferSource Buffers, PoseStack Transforms, Vec3[] Points, double[] Widths, int Light) {
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture));
        int Count = Points.length - 1;
        Vec3[][] Frames = Scp428Cords.frames(Points);
        Vec3[] Sides = Frames[0];
        Vec3[] Lifts = Frames[1];
        double[] Lengths = new double[Count + 1];
        for (int Index = 1; Index <= Count; Index++) {
            Lengths[Index] = Lengths[Index - 1] + Points[Index].distanceTo(Points[Index - 1]);
        }
        PoseStack.Pose Last = Transforms.last();
        for (int Index = 0; Index < Count; Index++) {
            double Near = Widths[Index] * 0.5;
            double Far = Widths[Index + 1] * 0.5;
            float NearV = (float) (Lengths[Index] * VPerBlock);
            float FarV = (float) (Lengths[Index + 1] * VPerBlock);
            for (int Face = 0; Face < 4; Face++) {
                Vec3 NearA = corner(Points[Index], Sides[Index], Lifts[Index], Face, Near);
                Vec3 NearB = corner(Points[Index], Sides[Index], Lifts[Index], Face + 1, Near);
                Vec3 FarA = corner(Points[Index + 1], Sides[Index + 1], Lifts[Index + 1], Face, Far);
                Vec3 FarB = corner(Points[Index + 1], Sides[Index + 1], Lifts[Index + 1], Face + 1, Far);
                Vec3 Normal = outward(Sides[Index], Lifts[Index], Face);
                float U0 = Face * 0.25f;
                float U1 = U0 + 0.25f;
                vertex(Buffer, Last, NearA, U0, NearV, Light, Normal);
                vertex(Buffer, Last, NearB, U1, NearV, Light, Normal);
                vertex(Buffer, Last, FarB, U1, FarV, Light, Normal);
                vertex(Buffer, Last, FarA, U0, FarV, Light, Normal);
            }
        }
    }

    private static Vec3 corner(Vec3 Centre, Vec3 Side, Vec3 Lift, int Face, double Half) {
        double Angle = Math.PI * 0.5 * (Face % 4) + Math.PI * 0.25;
        return Centre.add(Side.scale(Math.cos(Angle) * Half * 1.4142)).add(Lift.scale(Math.sin(Angle) * Half * 1.4142));
    }

    private static Vec3 outward(Vec3 Side, Vec3 Lift, int Face) {
        double Angle = Math.PI * 0.5 * (Face % 4) + Math.PI * 0.5;
        return Side.scale(Math.cos(Angle)).add(Lift.scale(Math.sin(Angle)));
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, Vec3 Point, float U, float V, int Light, Vec3 Normal) {
        Buffer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    private record Capture(Matrix4f Local, long At) {
    }
}
