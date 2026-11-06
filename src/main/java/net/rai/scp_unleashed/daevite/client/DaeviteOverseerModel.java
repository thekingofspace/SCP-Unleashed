package net.rai.scp_unleashed.daevite.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class DaeviteOverseerModel extends DClassGeoModel<DaeviteOverseer> {
    public static final ResourceLocation GeoPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/daevite/overseer.geo.json");
    public static final ResourceLocation ClipPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/daevite/overseer.animation.json");
    public static final ResourceLocation SkinPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/daevite/overseer.png");
    public static final String[] Hems = {"robe_front", "robe_back", "robe_left", "robe_right"};
    public static final float Flutter = 0.035f;

    @Override
    public ResourceLocation getModelResource(DaeviteOverseer Owner) {
        return GeoPath;
    }

    @Override
    public ResourceLocation getTextureResource(DaeviteOverseer Owner) {
        return SkinPath;
    }

    @Override
    public ResourceLocation getAnimationResource(DaeviteOverseer Owner) {
        return ClipPath;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(DaeviteOverseer Owner, long InstanceId, AnimationState<DaeviteOverseer> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner.isDeadOrDying()) {
            return;
        }
        float Time = Owner.tickCount + Animated.getPartialTick();
        for (int Index = 0; Index < Hems.length; Index++) {
            GeoBone Hem = part(Hems[Index]);
            if (Hem != null) {
                Hem.setRotX(Hem.getRotX() + Mth.sin(Time * 0.11f + Index * 1.7f) * Flutter);
            }
        }
    }
}
