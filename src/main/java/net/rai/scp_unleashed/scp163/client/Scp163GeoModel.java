package net.rai.scp_unleashed.scp163.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp163.Scp163Castaway;
import net.rai.scp_unleashed.scp163.Scp163Mood;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.EnumMap;
import java.util.Map;

public class Scp163GeoModel extends GeoModel<Scp163Castaway> {
    public static final String Name = "scp163_castaway";
    public static final String DomeBone = "dome";
    public static final String SuitPrefix = "suit_";
    public static final String VisorBone = "suit_visor";
    private static final ResourceLocation ModelPath = location("geo/entity/scp163/" + Name + ".geo.json");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp163/" + Name + ".animation.json");
    private static final Map<Scp163Mood, ResourceLocation> Textures = new EnumMap<>(Scp163Mood.class);

    static {
        for (Scp163Mood Mood : Scp163Mood.values()) {
            Textures.put(Mood, location("textures/entity/scp163/" + Name + "_" + Mood.key() + ".png"));
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static boolean isBareOnly(String Bone) {
        return Bone.equals("maw") || Bone.startsWith("feeder_") || Bone.startsWith("nub_") || Bone.startsWith("d_palm_") || Bone.startsWith("h_palm_");
    }

    @Override
    public ResourceLocation getModelResource(Scp163Castaway Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp163Castaway Creature) {
        return Textures.get(Creature.mood());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp163Castaway Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp163Castaway Creature, long InstanceId, AnimationState<Scp163Castaway> Animated) {
        boolean Suited = Creature.isSuited();
        for (GeoBone Bone : getAnimationProcessor().getRegisteredBones()) {
            String Label = Bone.getName();
            if (Label.startsWith(SuitPrefix)) {
                Bone.setHidden(!Suited);
            } else if (isBareOnly(Label)) {
                Bone.setHidden(Suited);
            }
        }
        GeoBone Dome = getAnimationProcessor().getBone(DomeBone);
        if (Dome != null) {
            float Partial = Animated.getPartialTick();
            float Across = Creature.domeAcross(Partial);
            Dome.setScaleX(Across);
            Dome.setScaleY(Creature.domeTall(Partial));
            Dome.setScaleZ(Across);
        }
    }
}
