package net.rai.scp_unleashed.scp111.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111Snail;
import net.rai.scp_unleashed.scp111.Scp111Variant;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.EnumMap;
import java.util.Map;

public class Scp111GeoModel extends GeoModel<Scp111Snail> {
    public static final String Name = "scp111_snail";
    public static final float LookYawShare = 0.6f;
    public static final float LookYawLimit = 35.0f;
    public static final float LookPitchLimit = 25.0f;
    private static final ResourceLocation AnimationPath = location("animations/entity/scp111/" + Name + ".animation.json");
    private static final Map<Scp111Variant, ResourceLocation> Models = new EnumMap<>(Scp111Variant.class);
    private static final Map<Scp111Variant, ResourceLocation> Textures = new EnumMap<>(Scp111Variant.class);

    static {
        for (Scp111Variant Variant : Scp111Variant.values()) {
            Models.put(Variant, location("geo/entity/scp111/" + Name + "_" + Variant.getSerializedName() + ".geo.json"));
            Textures.put(Variant, location("textures/entity/scp111/" + Name + "_" + Variant.getSerializedName() + ".png"));
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static ResourceLocation texture(Scp111Variant Variant) {
        return Textures.get(Variant);
    }

    @Override
    public ResourceLocation getModelResource(Scp111Snail Creature) {
        return Models.get(Creature.getVariant());
    }

    @Override
    public ResourceLocation getTextureResource(Scp111Snail Creature) {
        return Textures.get(Creature.getVariant());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp111Snail Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp111Snail Creature, long InstanceId, AnimationState<Scp111Snail> State) {
        EntityModelData Data = State.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data == null || Creature.isActing()) {
            return;
        }
        float Yaw = Mth.clamp(Data.netHeadYaw(), -LookYawLimit, LookYawLimit) * LookYawShare * Mth.DEG_TO_RAD;
        float Pitch = Mth.clamp(Data.headPitch(), -LookPitchLimit, LookPitchLimit) * Mth.DEG_TO_RAD;
        getBone("head").ifPresent(Head -> {
            Head.setRotY(Head.getRotY() + Yaw);
            Head.setRotX(Head.getRotX() + Pitch);
        });
    }
}
