package net.rai.scp_unleashed.scp131.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp131.Scp131Colour;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.EnumMap;
import java.util.Map;

public class Scp131GeoModel extends GeoModel<Scp131Pod> {
    public static final String Name = "scp131_pod";
    public static final float ModelScale = 0.5f;
    public static final ResourceLocation ModelPath = location("geo/entity/scp131/" + Name + ".geo.json");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp131/" + Name + ".animation.json");
    private static final Map<Scp131Colour, ResourceLocation> Textures = new EnumMap<>(Scp131Colour.class);

    static {
        for (Scp131Colour Colour : Scp131Colour.values()) {
            Textures.put(Colour, location("textures/entity/scp131/" + Name + "_" + Colour.getSerializedName() + ".png"));
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static ResourceLocation texture(Scp131Colour Colour) {
        return Textures.get(Colour);
    }

    @Override
    public ResourceLocation getModelResource(Scp131Pod Pod) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp131Pod Pod) {
        return texture(Pod.getColour());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp131Pod Pod) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp131Pod Pod, long InstanceId, AnimationState<Scp131Pod> State) {
        Scp131Rig.apply(this, Scp131Face.of(Pod).update(Pod, State.getPartialTick()));
    }
}
