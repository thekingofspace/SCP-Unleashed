package net.rai.scp_unleashed.scp001day.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp001DayFleshModel extends SharedGeoModel<Scp001DayFlesh> {
    private static final String[] Piles = {"pile_a", "pile_b", "pile_c"};
    private final Scp001DayForm Form;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;

    public Scp001DayFleshModel(Scp001DayForm Form) {
        this.Form = Form;
        ModelPath = path("geo/entity/scp001day/", Form.Key, ".geo.json");
        TexturePath = path("textures/entity/scp001day/", Form.Key, ".png");
        AnimationPath = path("animations/entity/scp001day/", Form.Key, ".animation.json");
    }

    private static ResourceLocation path(String Folder, String Key, String Suffix) {
        return Scp001DayRegistry.location(Folder + "scp001_day_" + Key + Suffix);
    }

    public static boolean pile(Scp001DayFlesh Owner) {
        return Owner.Form == Scp001DayForm.Mass && Owner.mass() >= Scp001DayForm.PileFloor;
    }

    public static String pileOf(Scp001DayFlesh Owner) {
        return Piles[Math.floorMod(Owner.getUUID().hashCode(), Scp001DayForm.PileVariants)];
    }

    @Override
    public ResourceLocation getModelResource(Scp001DayFlesh Owner) {
        return pile(Owner) ? path("geo/entity/scp001day/", pileOf(Owner), ".geo.json") : ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp001DayFlesh Owner) {
        return pile(Owner) ? path("textures/entity/scp001day/", pileOf(Owner), ".png") : TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp001DayFlesh Owner) {
        return pile(Owner) ? path("animations/entity/scp001day/", pileOf(Owner), ".animation.json") : AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp001DayFlesh Owner, long InstanceId, AnimationState<Scp001DayFlesh> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (!pile(Owner)) {
            return;
        }
        int Shown = Scp001DayForm.pileLimbs(Owner.mass());
        for (int Index = 0; Index < Scp001DayForm.PileLimbs; Index++) {
            GeoBone Limb = getAnimationProcessor().getBone("limb_" + Index);
            if (Limb != null) {
                Limb.setHidden(Index >= Shown);
            }
        }
    }
}
