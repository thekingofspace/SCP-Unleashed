package net.rai.scp_unleashed.scp096.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.rai.scp_unleashed.cameras.client.CameraClientHooks;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp096Faces {
    public static final float MinPixels = 4.0f;
    public static final float FacingDot = 0.3f;
    public static final float CensorFacing = -0.05f;
    public static final double WatchRange = 256.0;
    public static final float NearClip = 0.05f;
    public static final double RayPullback = 0.06;
    public static final int Forgotten = 200;
    public static final float MinFog = 2.0f;
    private static final float[][] FaceSamples = {{0.0f, 0.0f, -0.1f}, {2.0f, 0.0f, -0.1f}, {-2.0f, 0.0f, -0.1f}, {0.0f, 3.0f, -0.1f}, {0.0f, -2.0f, -0.1f}};
    private static final float[][] ChinSamples = {{0.0f, 1.0f, -0.1f}, {0.0f, -1.0f, -0.1f}};
    private static final float[][] FaceCorners = {{4.1f, 4.1f, -0.3f}, {-4.1f, 4.1f, -0.3f}, {4.1f, -5.1f, -0.3f}, {-4.1f, -5.1f, -0.3f}};
    private static final float[][] ChinCorners = {{2.6f, -2.1f, -0.3f}, {-2.6f, -2.1f, -0.3f}};
    private static final float[] Behind = {0.0f, 0.0f, 7.0f};
    private static final Map<Integer, Capture> Poses = new HashMap<>();
    private static final Set<Integer> SeenSinceTick = new HashSet<>();
    private static List<View> Views = List.of();
    private static long Frame;
    private static long ViewFrame = -1;

    private Scp096Faces() {
    }

    private static final class Capture {
        private final Matrix4f Face = new Matrix4f();
        private final Matrix4f Chin = new Matrix4f();
        private boolean HasFace;
        private boolean HasChin;
        private Vec3 Origin = Vec3.ZERO;
        private long Frame = -1;
    }

    public record View(int Id, boolean Seen, boolean Censor, float Left, float Top, float Right, float Bottom, float Size, float FaceSize, float Facing, int Visible) {
    }

    public static void beginFrame() {
        Frame++;
    }

    public static long frame() {
        return Frame;
    }

    public static boolean evaluatedThisFrame() {
        return ViewFrame == Frame;
    }

    public static void capture(Scp096ShyGuy Guy, boolean IsFace, Matrix4f Local, float Partial) {
        Capture Found = Poses.computeIfAbsent(Guy.getId(), Id -> new Capture());
        if (Found.Frame != Frame) {
            Found.Frame = Frame;
            Found.HasFace = false;
            Found.HasChin = false;
        }
        Found.Origin = Guy.getPosition(Partial);
        if (IsFace) {
            Found.Face.set(Local);
            Found.HasFace = true;
        } else {
            Found.Chin.set(Local);
            Found.HasChin = true;
        }
    }

    public static List<View> views() {
        return ViewFrame == Frame ? Views : List.of();
    }

    public static Set<Integer> takeSeen() {
        Set<Integer> Taken = new HashSet<>(SeenSinceTick);
        SeenSinceTick.clear();
        return Taken;
    }

    private static Vec3 place(Capture Found, Matrix4f Local, float[] Offset) {
        Vector3f Moved = Local.transformPosition(new Vector3f(-Offset[0] / 16.0f, Offset[1] / 16.0f, Offset[2] / 16.0f));
        return Found.Origin.add(Moved.x, Moved.y, Moved.z);
    }

    public static boolean watcherReady(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null || !User.isAlive() || User.isSpectator() || User.isCreative() || User.isSleeping() || Client.getCameraEntity() != User && !CameraClientHooks.isViewing()) {
            return false;
        }
        return Client.screen == null || Client.screen instanceof ChatScreen;
    }

    public static void evaluate(Minecraft Client, Vec3 Camera, Matrix4f ViewMatrix, Matrix4f Projection, float FogEnd) {
        ClientLevel World = Client.level;
        LocalPlayer User = Client.player;
        ViewFrame = Frame;
        if (World == null || User == null) {
            Views = List.of();
            return;
        }
        int Width = Client.getWindow().getWidth();
        int Height = Client.getWindow().getHeight();
        boolean Goggles = Scp096Persons.shielded(User);
        boolean Ready = watcherReady(Client) && !Goggles;
        boolean CensorWanted = Goggles && User.isAlive();
        Matrix4f Clip = new Matrix4f(Projection).mul(ViewMatrix);
        float FogLimit = FogEnd > MinFog ? FogEnd : Float.MAX_VALUE;
        List<View> Found = new ArrayList<>();
        Poses.values().removeIf(Each -> Frame - Each.Frame > Forgotten);
        for (Map.Entry<Integer, Capture> Entry : Poses.entrySet()) {
            Capture Each = Entry.getValue();
            if (Each.Frame != Frame || !Each.HasFace || !Each.HasChin) {
                continue;
            }
            Entity Known = World.getEntity(Entry.getKey());
            if (!(Known instanceof Scp096ShyGuy Guy) || !Guy.isAlive() || Guy.isBagged() || Guy.distanceToSqr(Camera) > WatchRange * WatchRange) {
                continue;
            }
            View Result = judge(World, Guy, Each, Camera, Clip, Width, Height, FogLimit, Ready, CensorWanted);
            if (Result != null) {
                Found.add(Result);
                if (Result.Seen()) {
                    SeenSinceTick.add(Guy.getId());
                }
            }
        }
        Views = Found;
    }

    private static View judge(ClientLevel World, Scp096ShyGuy Guy, Capture Each, Vec3 Camera, Matrix4f Clip, int Width, int Height, float FogEnd, boolean Ready, boolean CensorWanted) {
        Vec3 Centre = place(Each, Each.Face, FaceSamples[0]);
        Vec3 Back = place(Each, Each.Face, Behind);
        Vec3 Normal = Centre.subtract(Back);
        if (Normal.lengthSqr() < 1.0e-8) {
            return null;
        }
        Normal = Normal.normalize();
        Vec3 Toward = Camera.subtract(Centre);
        double Distance = Toward.length();
        if (Distance < 1.0e-4) {
            return null;
        }
        float Facing = (float) Normal.dot(Toward.scale(1.0 / Distance));
        if (Facing < CensorFacing) {
            return null;
        }
        List<Vec3> Samples = new ArrayList<>();
        for (float[] Offset : FaceSamples) {
            Samples.add(place(Each, Each.Face, Offset));
        }
        for (float[] Offset : ChinSamples) {
            Samples.add(place(Each, Each.Chin, Offset));
        }
        List<Vec3> Corners = new ArrayList<>();
        for (float[] Offset : FaceCorners) {
            Corners.add(place(Each, Each.Face, Offset));
        }
        for (float[] Offset : ChinCorners) {
            Corners.add(place(Each, Each.Chin, Offset));
        }
        float Left = Float.MAX_VALUE;
        float Top = Float.MAX_VALUE;
        float Right = -Float.MAX_VALUE;
        float Bottom = -Float.MAX_VALUE;
        boolean Crossing = false;
        boolean Ahead = false;
        List<Vec3> Outline = new ArrayList<>(Corners);
        Outline.addAll(Samples);
        List<Vector4f> Front = new ArrayList<>();
        List<Vector4f> Past = new ArrayList<>();
        for (Vec3 Point : Outline) {
            Vector4f Moved = clipSpace(Clip, Camera, Point);
            if (Moved.w <= NearClip) {
                Crossing = true;
                Past.add(Moved);
            } else {
                Front.add(Moved);
            }
        }
        List<float[]> Projected = new ArrayList<>();
        for (Vector4f Near : Front) {
            Ahead = true;
            Projected.add(toScreen(Near, Width, Height));
            for (Vector4f Far : Past) {
                float Share = (Near.w - NearClip) / (Near.w - Far.w);
                Projected.add(toScreen(new Vector4f(Near).lerp(Far, Share), Width, Height));
            }
        }
        if (!Ahead) {
            return null;
        }
        for (float[] Screen : Projected) {
            Left = Math.min(Left, Screen[0]);
            Right = Math.max(Right, Screen[0]);
            Top = Math.min(Top, Screen[1]);
            Bottom = Math.max(Bottom, Screen[1]);
        }
        if (Right < 0.0f || Bottom < 0.0f || Left > Width || Top > Height) {
            return null;
        }
        Left = Math.max(Left, 0.0f);
        Top = Math.max(Top, 0.0f);
        Right = Math.min(Right, Width);
        Bottom = Math.min(Bottom, Height);
        float Size = Math.max(Right - Left, Bottom - Top);
        float FaceLeft = Float.MAX_VALUE;
        float FaceTop = Float.MAX_VALUE;
        float FaceRight = -Float.MAX_VALUE;
        float FaceBottom = -Float.MAX_VALUE;
        int Visible = 0;
        boolean OnScreen = false;
        for (Vec3 Point : Samples) {
            float[] Screen = project(Clip, Camera, Point, Width, Height);
            if (Screen != null) {
                FaceLeft = Math.min(FaceLeft, Screen[0]);
                FaceRight = Math.max(FaceRight, Screen[0]);
                FaceTop = Math.min(FaceTop, Screen[1]);
                FaceBottom = Math.max(FaceBottom, Screen[1]);
            }
            if (Screen == null || Point.distanceTo(Camera) > FogEnd || !clear(World, Camera, Point)) {
                continue;
            }
            Visible++;
            if (Screen[0] >= 0.0f && Screen[0] <= Width && Screen[1] >= 0.0f && Screen[1] <= Height) {
                OnScreen = true;
            }
        }
        boolean Peeking = Visible > 0;
        if (!Peeking && CensorWanted) {
            for (Vec3 Point : Corners) {
                if (Point.distanceTo(Camera) <= FogEnd && project(Clip, Camera, Point, Width, Height) != null && clear(World, Camera, Point)) {
                    Peeking = true;
                    break;
                }
            }
        }
        float FaceSize = Crossing ? Float.MAX_VALUE : FaceRight >= FaceLeft ? Math.max(FaceRight - FaceLeft, FaceBottom - FaceTop) : 0.0f;
        boolean Seen = Ready && Guy.canBeSeen() && Facing >= FacingDot && FaceSize >= MinPixels && OnScreen;
        boolean Censor = CensorWanted && Peeking;
        if (!Seen && !Censor) {
            return null;
        }
        return new View(Guy.getId(), Seen, Censor, Left, Top, Right, Bottom, Size, FaceSize, Facing, Visible);
    }

    private static Vector4f clipSpace(Matrix4f Clip, Vec3 Camera, Vec3 Point) {
        Vector4f Moved = new Vector4f((float) (Point.x - Camera.x), (float) (Point.y - Camera.y), (float) (Point.z - Camera.z), 1.0f);
        Clip.transform(Moved);
        return Moved;
    }

    private static float[] toScreen(Vector4f Moved, int Width, int Height) {
        float X = (Moved.x / Moved.w * 0.5f + 0.5f) * Width;
        float Y = (1.0f - (Moved.y / Moved.w * 0.5f + 0.5f)) * Height;
        return new float[]{X, Y};
    }

    private static float[] project(Matrix4f Clip, Vec3 Camera, Vec3 Point, int Width, int Height) {
        Vector4f Moved = new Vector4f((float) (Point.x - Camera.x), (float) (Point.y - Camera.y), (float) (Point.z - Camera.z), 1.0f);
        Clip.transform(Moved);
        if (Moved.w <= NearClip) {
            return null;
        }
        float X = (Moved.x / Moved.w * 0.5f + 0.5f) * Width;
        float Y = (1.0f - (Moved.y / Moved.w * 0.5f + 0.5f)) * Height;
        return new float[]{X, Y};
    }

    private static boolean clear(ClientLevel World, Vec3 Camera, Vec3 Point) {
        Vec3 Toward = Camera.subtract(Point);
        double Length = Toward.length();
        if (Length < RayPullback * 2.0) {
            return true;
        }
        Vec3 End = Point.add(Toward.scale(RayPullback / Length));
        return World.clip(new ClipContext(Camera, End, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    public static void clearAll() {
        Poses.clear();
        SeenSinceTick.clear();
        Views = List.of();
    }
}
