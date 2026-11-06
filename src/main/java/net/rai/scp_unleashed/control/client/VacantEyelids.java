package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class VacantEyelids {
    public static final int MaxTries = 40;
    private static final int[] Missing = new int[0];
    private static final Map<ResourceLocation, int[]> Colours = new HashMap<>();
    private static final Map<ResourceLocation, Integer> Tries = new HashMap<>();
    private static final Set<ResourceLocation> Wanted = new LinkedHashSet<>();

    private VacantEyelids() {
    }

    @Nullable
    public static int[] colours(ResourceLocation Skin) {
        int[] Found = Colours.get(Skin);
        if (Found == null) {
            Wanted.add(Skin);
            return null;
        }
        return Found.length == 0 ? null : Found;
    }

    public static void clear() {
        Colours.clear();
        Tries.clear();
        Wanted.clear();
    }

    public static void tick() {
        if (Wanted.isEmpty() || !RenderSystem.isOnRenderThread()) {
            return;
        }
        ResourceLocation Skin = Wanted.iterator().next();
        Wanted.remove(Skin);
        int[] Sampled = sample(Skin);
        if (Sampled != null) {
            Colours.put(Skin, Sampled);
            return;
        }
        int Count = Tries.merge(Skin, 1, Integer::sum);
        if (Count >= MaxTries) {
            Colours.put(Skin, Missing);
        }
    }

    @Nullable
    private static int[] sample(ResourceLocation Skin) {
        AbstractTexture Texture = Minecraft.getInstance().getTextureManager().getTexture(Skin, null);
        if (Texture == null) {
            return null;
        }
        try (NativeImage Image = new NativeImage(64, 64, false)) {
            int Previous = GlStateManager._getInteger(32873);
            GlStateManager._bindTexture(Texture.getId());
            Image.downloadTexture(0, false);
            GlStateManager._bindTexture(Previous);
            int RightLid = pick(Image, 9, 10);
            int LeftLid = pick(Image, 13, 14);
            if (RightLid == 0 || LeftLid == 0) {
                return null;
            }
            return new int[]{RightLid & 0xFFFFFF, LeftLid & 0xFFFFFF, darker(RightLid), darker(LeftLid)};
        } catch (Throwable Problem) {
            return null;
        }
    }

    private static int pick(NativeImage Image, int First, int Second) {
        int Above = average(Image.getPixelRGBA(First, 11), Image.getPixelRGBA(Second, 11));
        int Below = average(Image.getPixelRGBA(First, 13), Image.getPixelRGBA(Second, 13));
        if (alpha(Above) > 0 && brightness(Above) >= 60) {
            return Above;
        }
        if (alpha(Below) > 0) {
            return Below;
        }
        return alpha(Above) > 0 ? Above : 0;
    }

    private static int alpha(int Abgr) {
        return Abgr >>> 24;
    }

    private static int average(int First, int Second) {
        int Red = (FastColor.ABGR32.red(First) + FastColor.ABGR32.red(Second)) / 2;
        int Green = (FastColor.ABGR32.green(First) + FastColor.ABGR32.green(Second)) / 2;
        int Blue = (FastColor.ABGR32.blue(First) + FastColor.ABGR32.blue(Second)) / 2;
        int Alpha = Math.min(alpha(First), alpha(Second));
        return Alpha << 24 | Red << 16 | Green << 8 | Blue;
    }

    private static int brightness(int Argb) {
        return ((Argb >> 16 & 255) + (Argb >> 8 & 255) + (Argb & 255)) / 3;
    }

    private static int darker(int Argb) {
        int Red = (Argb >> 16 & 255) * 70 / 100;
        int Green = (Argb >> 8 & 255) * 62 / 100;
        int Blue = (Argb & 255) * 60 / 100;
        return Red << 16 | Green << 8 | Blue;
    }
}
