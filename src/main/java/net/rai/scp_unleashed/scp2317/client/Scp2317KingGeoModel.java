package net.rai.scp_unleashed.scp2317.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp2317.Scp2317Chains;
import net.rai.scp_unleashed.scp2317.Scp2317KingBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.regex.Pattern;

public class Scp2317KingGeoModel extends SharedGeoModel<Scp2317KingBlockEntity> {
    public static final ResourceLocation ModelPath = Scp2317Registry.location("geo/entity/scp2317/scp2317_stone_king.geo.json");
    public static final ResourceLocation AnimationPath = Scp2317Registry.location("animations/entity/scp2317/scp2317_dormant_king.animation.json");
    public static final String[] HiddenBones = {"halo", "chain_a1", "chain_a2", "chain_b1", "chain_b2"};
    public static final Pattern EyeParts = Pattern.compile("^(eye|reye|seye)\\d+_(pupil|lid)$");
    private static final ResourceLocation[] Textures = new ResourceLocation[Scp2317Chains.count() + 1];

    static {
        for (int Index = 0; Index < Textures.length; Index++) {
            Textures[Index] = Scp2317Registry.location("textures/entity/scp2317/stone_king_" + Index + ".png");
        }
    }

    @Override
    public ResourceLocation getModelResource(Scp2317KingBlockEntity King) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp2317KingBlockEntity King) {
        return Textures[Math.max(0, Math.min(Textures.length - 1, King.broken()))];
    }

    @Override
    public ResourceLocation getAnimationResource(Scp2317KingBlockEntity King) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp2317KingBlockEntity King, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public void setCustomAnimations(Scp2317KingBlockEntity King, long InstanceId, AnimationState<Scp2317KingBlockEntity> Animated) {
        for (String Name : HiddenBones) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setHidden(true);
            }
        }
        for (GeoBone Found : getAnimationProcessor().getRegisteredBones()) {
            if (EyeParts.matcher(Found.getName()).matches()) {
                Found.setHidden(true);
            }
        }
    }
}
