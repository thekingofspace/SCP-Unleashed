package net.rai.scp_unleashed.scp682.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp682.Scp682;
import net.rai.scp_unleashed.scp682.Scp682Adaptation;
import net.rai.scp_unleashed.scp682.Scp682Registry;
import net.rai.scp_unleashed.scp682.Scp682Signature;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Scp682OverlayLayer extends GeoRenderLayer<Scp682> {
    public static final float ShedTicks = 30.0f;
    public static final float FadedAlpha = 0.6f;
    public static final float Settled = 1000.0f;
    public static final int Remembered = 256;
    private static final ResourceLocation[] Hides = hides();
    private static final List<Scp682Signature> Painted = List.of(Scp682Signature.Cinder, Scp682Signature.Rime, Scp682Signature.RedIce, Scp682Signature.Plague,
            Scp682Signature.Insulated, Scp682Signature.Scarred);
    private static final ResourceLocation[] Marks = marks();
    private static final int Categories = Scp682Adaptation.Category.values().length;
    private static final int Signatures = Scp682Signature.values().length;
    private final Map<Integer, float[]> Shedding = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, float[]> Eldest) {
            return size() > Remembered;
        }
    };

    public Scp682OverlayLayer(GeoRenderer<Scp682> Renderer) {
        super(Renderer);
    }

    private static ResourceLocation[] hides() {
        Scp682Adaptation.Category[] All = Scp682Adaptation.Category.values();
        ResourceLocation[] Found = new ResourceLocation[All.length];
        for (int Index = 0; Index < All.length; Index++) {
            Found[Index] = Scp682Registry.location("textures/entity/scp682/hide_" + All[Index].name().toLowerCase(Locale.ROOT) + ".png");
        }
        return Found;
    }

    private static ResourceLocation[] marks() {
        ResourceLocation[] Found = new ResourceLocation[Scp682Signature.values().length];
        for (Scp682Signature Each : Painted) {
            Found[Each.ordinal()] = Scp682Registry.location("textures/entity/scp682/sig_" + Each.key() + ".png");
        }
        return Found;
    }

    private float[] shedding(Scp682 Owner, int Mask, int Held, float Now) {
        float[] Found = Shedding.get(Owner.getId());
        if (Found == null) {
            Found = new float[Categories + Signatures + 2];
            Arrays.fill(Found, Now - Settled);
            Found[Categories + Signatures] = Mask;
            Found[Categories + Signatures + 1] = Held;
            Shedding.put(Owner.getId(), Found);
        }
        int MaskBefore = (int) Found[Categories + Signatures];
        int HeldBefore = (int) Found[Categories + Signatures + 1];
        for (Scp682Adaptation.Category Each : Scp682Adaptation.Category.values()) {
            if ((MaskBefore & Each.bit()) != 0 && (Mask & Each.bit()) == 0) {
                Found[Each.ordinal()] = Now;
            }
        }
        for (Scp682Signature Each : Scp682Signature.values()) {
            if ((HeldBefore & Each.bit()) != 0 && (Held & Each.bit()) == 0) {
                Found[Categories + Each.ordinal()] = Now;
            }
        }
        Found[Categories + Signatures] = Mask;
        Found[Categories + Signatures + 1] = Held;
        return Found;
    }

    private static float shedAlpha(float Now, float LostAt) {
        float Age = Now - LostAt;
        if (Age < 0.0f || Age >= ShedTicks) {
            return 0.0f;
        }
        return FadedAlpha * (1.0f - Mth.clamp(Age / ShedTicks, 0.0f, 1.0f));
    }

    @Override
    public void render(PoseStack Pose, Scp682 Owner, BakedGeoModel Model, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float PartialTick, int Light,
                       int Overlay) {
        int Mask = Owner.hide();
        int Held = Owner.signatures();
        int FadingMask = Owner.fadingHide();
        int FadingHeld = Owner.fadingSignatures();
        float Now = Owner.tickCount + PartialTick;
        float[] Lost = shedding(Owner, Mask, Held, Now);
        for (Scp682Adaptation.Category Each : Scp682Adaptation.Category.values()) {
            float Alpha = (Mask & Each.bit()) != 0 ? (FadingMask & Each.bit()) != 0 ? FadedAlpha : 1.0f : shedAlpha(Now, Lost[Each.ordinal()]);
            draw(Pose, Owner, Model, Buffers, PartialTick, Light, Overlay, Hides[Each.ordinal()], Alpha);
        }
        for (Scp682Signature Each : Painted) {
            float Alpha = (Held & Each.bit()) != 0 ? (FadingHeld & Each.bit()) != 0 ? FadedAlpha : 1.0f : shedAlpha(Now, Lost[Categories + Each.ordinal()]);
            draw(Pose, Owner, Model, Buffers, PartialTick, Light, Overlay, Marks[Each.ordinal()], Alpha);
        }
    }

    private void draw(PoseStack Pose, Scp682 Owner, BakedGeoModel Model, MultiBufferSource Buffers, float PartialTick, int Light, int Overlay, ResourceLocation Texture,
                      float Alpha) {
        if (Alpha <= 0.0f) {
            return;
        }
        if (Alpha >= 1.0f) {
            RenderType Layer = RenderType.entityCutoutNoCull(Texture);
            getRenderer().reRender(Model, Pose, Buffers, Owner, Layer, Buffers.getBuffer(Layer), PartialTick, Light, Overlay, -1);
            return;
        }
        RenderType Layer = RenderType.entityTranslucent(Texture);
        getRenderer().reRender(Model, Pose, Buffers, Owner, Layer, Buffers.getBuffer(Layer), PartialTick, Light, Overlay,
                FastColor.ARGB32.color(Mth.clamp(Math.round(Alpha * 255.0f), 0, 255), 0xFFFFFF));
    }
}
