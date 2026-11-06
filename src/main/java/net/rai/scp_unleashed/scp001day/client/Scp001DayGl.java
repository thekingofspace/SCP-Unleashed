package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySunUpload;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;

public final class Scp001DayGl {
    private static final int[] UnpackKeys = {GL11C.GL_UNPACK_ROW_LENGTH, GL12C.GL_UNPACK_IMAGE_HEIGHT, GL11C.GL_UNPACK_SKIP_PIXELS, GL11C.GL_UNPACK_SKIP_ROWS,
            GL12C.GL_UNPACK_SKIP_IMAGES, GL11C.GL_UNPACK_ALIGNMENT};
    private static final int[] PackKeys = {GL11C.GL_PACK_ROW_LENGTH, GL11C.GL_PACK_SKIP_PIXELS, GL11C.GL_PACK_SKIP_ROWS, GL11C.GL_PACK_ALIGNMENT};

    private Scp001DayGl() {
    }

    public static int[] clean() {
        Scp001DaySunUpload.Unpack State = Scp001DaySunUpload.Clean;
        return new int[]{State.RowLength(), State.ImageHeight(), State.SkipPixels(), State.SkipRows(), State.SkipImages(), State.Alignment()};
    }

    public static void unpacked(int Target, int Texture, Runnable Upload) {
        RenderSystem.assertOnRenderThreadOrInit();
        int[] Saved = new int[UnpackKeys.length];
        for (int Index = 0; Index < UnpackKeys.length; Index++) {
            Saved[Index] = GL11C.glGetInteger(UnpackKeys[Index]);
        }
        int Buffer = GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int Binding = GL11C.glGetInteger(Target == GL12C.GL_TEXTURE_3D ? GL12C.GL_TEXTURE_BINDING_3D : GL11C.GL_TEXTURE_BINDING_2D);
        if (Buffer != 0) {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);
        }
        int[] Clean = clean();
        for (int Index = 0; Index < UnpackKeys.length; Index++) {
            GL11C.glPixelStorei(UnpackKeys[Index], Clean[Index]);
        }
        GL11C.glBindTexture(Target, Texture);
        try {
            Upload.run();
        } finally {
            GL11C.glBindTexture(Target, Binding);
            for (int Index = 0; Index < UnpackKeys.length; Index++) {
                GL11C.glPixelStorei(UnpackKeys[Index], Saved[Index]);
            }
            if (Buffer != 0) {
                GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, Buffer);
            }
        }
    }

    public static void packed(Runnable Read) {
        RenderSystem.assertOnRenderThreadOrInit();
        int[] Saved = new int[PackKeys.length];
        for (int Index = 0; Index < PackKeys.length; Index++) {
            Saved[Index] = GL11C.glGetInteger(PackKeys[Index]);
        }
        int Buffer = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
        if (Buffer != 0) {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, 0);
        }
        for (int Index = 0; Index < PackKeys.length; Index++) {
            GL11C.glPixelStorei(PackKeys[Index], PackKeys[Index] == GL11C.GL_PACK_ALIGNMENT ? 4 : 0);
        }
        try {
            Read.run();
        } finally {
            for (int Index = 0; Index < PackKeys.length; Index++) {
                GL11C.glPixelStorei(PackKeys[Index], Saved[Index]);
            }
            if (Buffer != 0) {
                GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, Buffer);
            }
        }
    }
}
