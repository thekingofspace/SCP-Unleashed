package net.rai.scp_unleashed.stranger;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StrangerVariants {
    public static final String FoundationId = "foundation";
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, StrangerVariant> Variants = new ConcurrentHashMap<>();
    public static final ResourceLocation SharedModel = location("geo/entity/stranger/figure.geo.json");
    public static final ResourceLocation SharedAnimations = location("animations/entity/stranger/figure.animation.json");
    public static final ResourceLocation SharedTexture = location("textures/entity/stranger/foundation.png");
    public static final String CourierPlate = "gui.scp_unleashed.stranger.plate.foundation";

    public static final StrangerVariant Foundation = register(new StrangerVariant(FoundationId, SharedModel, SharedAnimations,
            SharedTexture, location("textures/gui/stranger/portrait_foundation.png"),
            "entity.scp_unleashed.stranger_figure.foundation", CourierPlate,
            "stranger.scp_unleashed.foundation", 3, List.of(), 0x1E2A38, 0xC6DCFF, 0xD2D9E2, 0.84f));

    private StrangerVariants() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static StrangerVariant register(StrangerVariant Variant) {
        StrangerVariant Previous = Variants.put(Variant.Id(), Variant);
        if (Previous != null) {
            Log.warn("Stranger variant {} was replaced", Variant.Id());
        }
        return Variant;
    }

    public static boolean exists(String Id) {
        return Variants.containsKey(Id);
    }

    public static StrangerVariant get(String Id) {
        StrangerVariant Found = Variants.get(Id);
        return Found != null ? Found : Foundation;
    }

    public static Set<String> ids() {
        return Set.copyOf(Variants.keySet());
    }
}
