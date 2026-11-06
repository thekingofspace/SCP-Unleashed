package net.rai.scp_unleashed.z0004.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.z0004.Z0004DeckSlot;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public class Z0004DeckGeoModel extends SharedGeoModel<Z0004DeckHand> {
    public static final List<String> Props = List.of("prop_mop", "prop_rammer");

    @Override
    public ResourceLocation getModelResource(Z0004DeckHand Hand) {
        return Hand.slot().captain() ? Z0004PirateGeoModel.CaptainModel : Z0004PirateGeoModel.Models[Hand.slot().Variant()];
    }

    @Override
    public ResourceLocation getTextureResource(Z0004DeckHand Hand) {
        return Hand.slot().captain() ? Z0004PirateGeoModel.CaptainTexture : Z0004PirateGeoModel.Textures[Hand.slot().Variant()];
    }

    @Override
    public ResourceLocation getAnimationResource(Z0004DeckHand Hand) {
        return Hand.slot().captain() ? Z0004PirateGeoModel.CaptainAnimation : Z0004PirateGeoModel.Animations[Hand.slot().Variant()];
    }

    @Override
    public void setCustomAnimations(Z0004DeckHand Hand, long InstanceId, AnimationState<Z0004DeckHand> Animated) {
        super.setCustomAnimations(Hand, InstanceId, Animated);
        String Held = Hand.slot().Prop();
        boolean Using = !Held.isEmpty() && !Hand.ship().isDeadOrDying();
        for (String Name : Props) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setHidden(!(Using && Name.equals(Held)));
            }
        }
        GeoBone Weapon = getAnimationProcessor().getBone("weapon_right");
        if (Weapon != null) {
            Weapon.setHidden(Using);
        }
    }
}
