package net.rai.scp_unleashed.scp1730.pit.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.pit.Scp1730Mass;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitForm;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitTongue;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp1730PitModel<T extends Entity & GeoAnimatable> extends SharedGeoModel<T> {
    public static final float[] PitLids = {4.2f, 4.2f, 4.2f, 4.2f, 4.2f, 4.2f, 3.2f, 2.2f, 2.2f, 2.2f};
    public static final float[] MassLids = {3.2f, 3.2f, 3.2f, 3.2f, 3.2f, 3.2f, 2.2f, 2.2f, 2.2f, 2.2f, 2.2f};
    private final ResourceLocation ModelPath;
    private final ResourceLocation[] Textures;
    private final ResourceLocation AnimationPath;

    public Scp1730PitModel(String Name, int Stages) {
        this.ModelPath = Scp1730Registry.location("geo/entity/scp1730/" + Name + ".geo.json");
        this.AnimationPath = Scp1730Registry.location("animations/entity/scp1730/" + Name + ".animation.json");
        this.Textures = new ResourceLocation[Math.max(1, Stages)];
        for (int Stage = 0; Stage < Textures.length; Stage++) {
            Textures[Stage] = Scp1730Registry.location("textures/entity/scp1730/" + Name + (Stage == 0 ? "" : "_cracked" + Stage) + ".png");
        }
    }

    @Override
    public ResourceLocation getModelResource(T Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Owner) {
        int Stage = Owner instanceof Scp1730PitForm Pit ? Pit.stage() : 0;
        return Textures[Mth.clamp(Stage, 0, Textures.length - 1)];
    }

    @Override
    public ResourceLocation getAnimationResource(T Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Owner, long InstanceId, AnimationState<T> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner instanceof Scp1730PitForm Pit) {
            lids(PitLids, Pit.closedEyes());
            GeoBone Tongue = getAnimationProcessor().getBone("tongue1");
            if (Tongue != null && Pit.action() == Scp1730PitForm.ActionTongue) {
                Tongue.setHidden(true);
                Tongue.setChildrenHidden(true);
            }
            return;
        }
        if (Owner instanceof Scp1730Mass Mass) {
            int Closed = Mass.isDeadOrDying() ? MassLids.length : Mass.digging() ? 3 : 0;
            lids(MassLids, Closed);
            return;
        }
        if (Owner instanceof Scp1730PitTongue Tongue) {
            GeoBone Root = getAnimationProcessor().getBone("tongue");
            if (Root != null) {
                float Length = Mth.lerp(Animated.getPartialTick(), Tongue.LengthOld, Tongue.length());
                Root.setScaleZ(Root.getInitialSnapshot().getScaleZ() * Math.max(0.05f, Length / Scp1730PitTongue.BaseLength));
            }
        }
    }

    private void lids(float[] Sizes, int Closed) {
        for (int Index = 0; Index < Sizes.length; Index++) {
            GeoBone Lid = getAnimationProcessor().getBone("lid" + (Index + 1));
            if (Lid == null) {
                continue;
            }
            boolean Shut = Index < Closed;
            Lid.setScaleY(Lid.getInitialSnapshot().getScaleY() * (Shut ? Sizes[Index] : 1.0f));
        }
    }
}
