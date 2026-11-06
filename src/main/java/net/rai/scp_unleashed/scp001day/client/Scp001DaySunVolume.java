package net.rai.scp_unleashed.scp001day.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;

public final class Scp001DaySunVolume {
    public static final int SizeX = 128;
    public static final int SizeY = 80;
    public static final int SizeZ = 128;
    public static final int LayersPerFrame = 16;
    public static final int Recentre = 8;
    private static final int[] Textures = new int[2];
    private static int Framebuffer;
    private static int Front;
    private static boolean Ready;
    private static boolean Building;
    private static int Layer;
    private static int BuildX;
    private static int BuildY;
    private static int BuildZ;
    private static long BuildEpoch;
    private static int BuildVersion;
    private static int FrontX;
    private static int FrontY;
    private static int FrontZ;
    private static long FrontEpoch = Long.MIN_VALUE;
    private static int FrontVersion = Integer.MIN_VALUE;

    private Scp001DaySunVolume() {
    }

    public static boolean ready() {
        return Ready;
    }

    public static int texture() {
        return Textures[Front];
    }

    public static int originX() {
        return FrontX;
    }

    public static int originY() {
        return FrontY;
    }

    public static int originZ() {
        return FrontZ;
    }

    public static float extentX() {
        return SizeX * 0.5f;
    }

    public static float extentY() {
        return SizeY * 0.5f;
    }

    public static float extentZ() {
        return SizeZ * 0.5f;
    }

    public static int progress() {
        return Ready && !Building ? 100 : Math.round(100.0f * Layer / SizeZ);
    }

    public static boolean update(Vec3 Eye, long Epoch, int GridVersion, ShaderProgram Program, Consumer<ShaderProgram> Bind, int GridX, int GridY, int GridZ) {
        if (!ensure()) {
            return true;
        }
        int WantX = Math.floorDiv((int) Math.floor(Eye.x) - SizeX / 4, 4) * 4;
        int WantY = Math.floorDiv((int) Math.floor(Eye.y) - SizeY / 4, 4) * 4;
        int WantZ = Math.floorDiv((int) Math.floor(Eye.z) - SizeZ / 4, 4) * 4;
        boolean Stale = !Ready || Epoch != FrontEpoch || GridVersion != FrontVersion || Math.abs(WantX - FrontX) > Recentre || Math.abs(WantY - FrontY) > Recentre || Math.abs(WantZ - FrontZ) > Recentre;
        if (!Building && Stale) {
            Building = true;
            Layer = 0;
            BuildX = WantX;
            BuildY = WantY;
            BuildZ = WantZ;
            BuildEpoch = Epoch;
            BuildVersion = GridVersion;
        }
        if (!Building || Program == null) {
            return true;
        }
        int Back = 1 - Front;
        int[] Viewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, Viewport);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, Framebuffer);
        GL11C.glViewport(0, 0, SizeX, SizeY);
        Bind.accept(Program);
        Program.getUniformSafe("VolumeOrigin").setVector(BuildX - GridX, BuildY - GridY, BuildZ - GridZ);
        int End = Math.min(SizeZ, Layer + LayersPerFrame);
        boolean Bound = Scp001DayShafts.pass(Program, () -> {
            for (; Layer < End; Layer++) {
                GL30C.glFramebufferTextureLayer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, Textures[Back], 0, Layer);
                Program.getUniformSafe("VolumeLayer").setFloat(Layer);
                VeilRenderSystem.drawScreenQuad();
            }
        });
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        GL11C.glViewport(Viewport[0], Viewport[1], Viewport[2], Viewport[3]);
        if (Layer >= SizeZ) {
            Building = false;
            Front = Back;
            FrontX = BuildX;
            FrontY = BuildY;
            FrontZ = BuildZ;
            FrontEpoch = BuildEpoch;
            FrontVersion = BuildVersion;
            Ready = true;
        }
        return Bound;
    }

    private static boolean ensure() {
        for (int Index = 0; Index < 2; Index++) {
            if (Textures[Index] == 0) {
                Textures[Index] = makeTexture();
            }
        }
        if (Framebuffer == 0) {
            Framebuffer = GL30C.glGenFramebuffers();
        }
        return Framebuffer != 0 && Textures[0] != 0 && Textures[1] != 0;
    }

    private static int makeTexture() {
        int Id = GL11C.glGenTextures();
        if (Id == 0) {
            return 0;
        }
        Scp001DayGl.unpacked(GL12C.GL_TEXTURE_3D, Id, () -> {
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_BORDER);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_BORDER);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_WRAP_R, GL13C.GL_CLAMP_TO_BORDER);
            GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D, 0, GL30C.GL_R8, SizeX, SizeY, SizeZ, 0, GL11C.GL_RED, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        });
        return Id;
    }

    public static boolean holding() {
        return Framebuffer != 0 || Textures[0] != 0 || Textures[1] != 0;
    }

    public static void release() {
        if (Framebuffer != 0) {
            GL30C.glDeleteFramebuffers(Framebuffer);
            Framebuffer = 0;
        }
        for (int Index = 0; Index < 2; Index++) {
            if (Textures[Index] != 0) {
                GL11C.glDeleteTextures(Textures[Index]);
                Textures[Index] = 0;
            }
        }
        Ready = false;
        Building = false;
        FrontEpoch = Long.MIN_VALUE;
    }
}
