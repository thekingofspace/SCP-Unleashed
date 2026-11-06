package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class DClassSkins {
    public static final int Width = 128;
    public static final int Height = 64;
    public static final int Levels = 6;
    public static final int[][] SkinTones = {
            {0x8A5A44, 0xA86E52, 0xC4876A, 0xD99F80, 0xEAB898, 0xF6D2B6},
            {0x7A4A30, 0x96603F, 0xB27850, 0xC88E62, 0xDCA476, 0xEBBC8E},
            {0x643A22, 0x7E4C2E, 0x98613C, 0xAE744A, 0xC28A5C, 0xD4A070},
            {0x4C2B19, 0x603822, 0x77482C, 0x8C5836, 0x9E6842, 0xB07A52},
            {0x38200F, 0x482A16, 0x5A361E, 0x6B4226, 0x7C5030, 0x8E5E3A},
            {0x26150A, 0x321C0F, 0x402514, 0x4E2E1A, 0x5C3820, 0x6C4428}};
    public static final int[][] HairTones = {
            {0x0E0C0C, 0x16130F, 0x1E1A15, 0x29231C, 0x352D24, 0x433829},
            {0x24160C, 0x301E10, 0x3E2716, 0x4C311C, 0x5C3C23, 0x6C482B},
            {0x3E2412, 0x523018, 0x663D1F, 0x7A4B27, 0x8E5A30, 0xA26A3A},
            {0x4E1C0E, 0x662612, 0x803218, 0x983E1E, 0xAE4C26, 0xC25C30},
            {0x7A5A26, 0x977232, 0xB08C42, 0xC6A456, 0xD8BA6C, 0xE6CE86},
            {0x4A4A4C, 0x626264, 0x7A7A7C, 0x929294, 0xAAAAAC, 0xC4C4C6}};
    public static final int[] IrisColours = {0x4A2C18, 0x2A1C14, 0x3A6EA8, 0x4A7A36, 0x6A7884, 0x7A5E2A};
    public static final int[][] PupilCells = {{72, 16}, {73, 16}};
    public static final int[] PupilFaces = {6};
    private static final String Folder = "textures/entity/dclass/";
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<Integer, ResourceLocation> Cache = new HashMap<>();
    private static final Map<String, NativeImage> Parts = new HashMap<>();
    private static final ResourceLocation Fallback = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Folder + "fallback.png");

    private DClassSkins() {
    }

    public static ResourceLocation textureFor(int Code) {
        ResourceLocation Found = Cache.get(Code);
        if (Found != null) {
            return Found;
        }
        NativeImage Made = compose(Code);
        if (Made == null) {
            return Fallback;
        }
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dynamic/dclass_" + Integer.toHexString(Code));
        Minecraft.getInstance().getTextureManager().register(Id, new DynamicTexture(Made));
        Cache.put(Code, Id);
        return Id;
    }

    public static void reset() {
        for (ResourceLocation Id : Cache.values()) {
            Minecraft.getInstance().getTextureManager().release(Id);
        }
        Cache.clear();
        for (NativeImage Image : Parts.values()) {
            Image.close();
        }
        Parts.clear();
    }

    @Nullable
    private static NativeImage part(String Name) {
        if (Parts.containsKey(Name)) {
            return Parts.get(Name);
        }
        NativeImage Loaded = null;
        ResourceManager Manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> Found = Manager.getResource(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Folder + Name + ".png"));
        if (Found.isPresent()) {
            try (InputStream Stream = Found.get().open()) {
                Loaded = NativeImage.read(Stream);
            } catch (Exception Problem) {
                Log.warn("Could not read D-Class texture part {}", Name, Problem);
            }
        }
        Parts.put(Name, Loaded);
        return Loaded;
    }

    @Nullable
    public static NativeImage compose(int Code) {
        NativeImage Skin = part("skin");
        NativeImage Suit = part(DClassAppearance.suit(Code) == DClassAppearance.Fatigues ? "suit_fatigues" : "suit_jumpsuit");
        if (Skin == null || Suit == null) {
            return null;
        }
        int[] SkinRamp = SkinTones[DClassAppearance.skin(Code)];
        int[] HairRamp = HairTones[DClassAppearance.hairColour(Code)];
        NativeImage Result = new NativeImage(Width, Height, true);
        layer(Result, Skin, SkinRamp);
        layer(Result, Suit, SkinRamp);
        int Facial = DClassAppearance.facial(Code);
        if (Facial > 0) {
            layer(Result, part("facial_" + Facial), HairRamp);
        }
        layer(Result, part("brows_" + DClassAppearance.brow(Code)), HairRamp);
        int Style = DClassAppearance.style(Code);
        if (Style > 0) {
            layer(Result, part("hair_" + Style), HairRamp);
        }
        int Mark = DClassAppearance.mark(Code);
        if (Mark > 0) {
            layer(Result, part("mark_" + Mark), SkinRamp);
        }
        int Iris = 0xFF000000 | IrisColours[DClassAppearance.eyeColour(Code)];
        for (int[] Cell : PupilCells) {
            Result.setPixelRGBA(Cell[0], Cell[1], toAbgr(Iris));
        }
        return Result;
    }

    private static void layer(NativeImage Target, @Nullable NativeImage Source, int[] Ramp) {
        if (Source == null) {
            return;
        }
        int Wide = Math.min(Target.getWidth(), Source.getWidth());
        int Tall = Math.min(Target.getHeight(), Source.getHeight());
        for (int Y = 0; Y < Tall; Y++) {
            for (int X = 0; X < Wide; X++) {
                int Abgr = Source.getPixelRGBA(X, Y);
                int Alpha = Abgr >>> 24;
                if (Alpha == 0) {
                    continue;
                }
                int Red = Abgr & 255;
                int Green = Abgr >> 8 & 255;
                int Blue = Abgr >> 16 & 255;
                int Argb;
                if (Red == Green && Green == Blue && Red % 32 == 0 && Red >= 32 && Red <= 32 * Levels) {
                    Argb = 0xFF000000 | Ramp[Red / 32 - 1];
                } else {
                    Argb = 0xFF000000 | Red << 16 | Green << 8 | Blue;
                }
                Target.setPixelRGBA(X, Y, toAbgr(Argb));
            }
        }
    }

    private static int toAbgr(int Argb) {
        int Alpha = Argb >>> 24;
        int Red = Argb >> 16 & 255;
        int Green = Argb >> 8 & 255;
        int Blue = Argb & 255;
        return Alpha << 24 | Blue << 16 | Green << 8 | Red;
    }
}
