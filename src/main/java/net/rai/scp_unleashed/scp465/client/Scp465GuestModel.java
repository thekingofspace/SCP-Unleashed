package net.rai.scp_unleashed.scp465.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp465.Scp465Guest;
import net.rai.scp_unleashed.scp465.Scp465Names;
import net.rai.scp_unleashed.scp465.Scp465Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp465GuestModel extends DClassGeoModel<Scp465Guest> {
    public static final ResourceLocation GuestModel = Scp465Registry.location("geo/entity/scp465/scp465_guest.geo.json");
    public static final ResourceLocation GuestAnimations = Scp465Registry.location("animations/entity/scp465/scp465_guest.animation.json");
    public static final ResourceLocation DjTexture = Scp465Registry.location("textures/entity/scp465/dj.png");
    public static final String[] ClownBones = {"clown_nose", "clown_ruff", "clown_wig", "right_shoe", "left_shoe"};
    public static final String[] GuestBones = {"party_hat"};
    public static final String[] DjBones = {"headphones"};
    private static final ResourceLocation[] ClownTextures = textures("clown", Scp465Names.ClownLooks);
    private static final ResourceLocation[] GuestTextures = textures("guest", Scp465Names.GuestLooks);

    private static ResourceLocation[] textures(String Name, int Count) {
        ResourceLocation[] Made = new ResourceLocation[Count];
        for (int Index = 0; Index < Count; Index++) {
            Made[Index] = Scp465Registry.location("textures/entity/scp465/" + Name + "_" + Index + ".png");
        }
        return Made;
    }

    public static boolean bun(int Look) {
        return Look == Scp465Names.GrandmaLook || Look == 2;
    }

    public static boolean ponytail(int Look) {
        return Look == 1 || Look == 4;
    }

    @Override
    public ResourceLocation getModelResource(Scp465Guest Owner) {
        return GuestModel;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp465Guest Owner) {
        return GuestAnimations;
    }

    @Override
    public ResourceLocation getTextureResource(Scp465Guest Owner) {
        int Look = Owner.look();
        return switch (Owner.kind()) {
            case Scp465Guest.KindClown -> ClownTextures[Math.floorMod(Look, ClownTextures.length)];
            case Scp465Guest.KindDj -> DjTexture;
            default -> GuestTextures[Math.floorMod(Look, GuestTextures.length)];
        };
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void show(String[] Names, boolean Shown) {
        for (String Name : Names) {
            GeoBone Found = part(Name);
            if (Found != null) {
                Found.setHidden(!Shown);
            }
        }
    }

    @Override
    public void setCustomAnimations(Scp465Guest Owner, long InstanceId, AnimationState<Scp465Guest> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        byte Kind = Owner.kind();
        int Look = Owner.look();
        show(ClownBones, Kind == Scp465Guest.KindClown);
        show(GuestBones, Kind == Scp465Guest.KindGuest);
        show(DjBones, Kind == Scp465Guest.KindDj);
        GeoBone Bun = part("hair_bun");
        if (Bun != null) {
            Bun.setHidden(Kind != Scp465Guest.KindGuest || !bun(Look));
        }
        GeoBone Tail = part("hair_ponytail");
        if (Tail != null) {
            Tail.setHidden(Kind != Scp465Guest.KindGuest || !ponytail(Look));
        }
    }
}
