package net.rai.scp_unleashed.seasonal;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SeasonalLooks {
    public static final String HeadBone = "head";
    public static final ResourceLocation PumpkinModel = location("geo/entity/seasonal/scp173_pumpkin.geo.json");
    public static final ResourceLocation PumpkinTexture = location("textures/entity/seasonal/scp173_pumpkin.png");
    public static final ResourceLocation PumpkinGlow = location("textures/entity/seasonal/scp173_pumpkin_glow.png");
    public static final float GlowSteps = 8.0f;
    public static final float GlowMinimum = 0.1f;
    public static final float DarkFrom = 12.0f;

    private SeasonalLooks() {
    }

    private static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static boolean scp173Pumpkin(int SyncedMask, boolean Invisible) {
        return !Invisible && Season.Halloween.in(SyncedMask);
    }

    public static float emberGlow(int BlockLight, int SkyLight, int SkyDarken) {
        float Brightest = Math.max(0.0f, Math.max(BlockLight, SkyLight - SkyDarken));
        float Dark = Math.max(0.0f, DarkFrom - Brightest) / DarkFrom;
        return Math.round(Dark * GlowSteps) / GlowSteps;
    }

    public static boolean emberShown(float Glow, boolean ShadowPass, boolean Petrified) {
        return Glow >= GlowMinimum && !ShadowPass && !Petrified;
    }
}
