package net.rai.scp_unleashed.guns.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunTimer;
import org.jetbrains.annotations.Nullable;

public final class GunReloadStage {
    public static final float Open = 0.5F;
    public static final float Empty = 1.0F;
    public static final float MagazineOut = 0.12F;
    public static final float MagazineIn = 0.62F;
    public static final float CoverOpen = 0.03F;
    public static final float BoxOut = 0.2F;
    public static final float BoxIn = 0.55F;
    public static final float CoverClosed = 0.88F;

    private GunReloadStage() {
    }

    public static float stage(GunKind Kind, float Progress) {
        return switch (Kind) {
            case CombatShotgun -> Progress >= MagazineOut && Progress < MagazineIn ? Empty : 0.0F;
            case MachineGun -> Progress < CoverOpen || Progress >= CoverClosed ? 0.0F : Progress >= BoxOut && Progress < BoxIn ? Empty : Open;
            default -> 0.0F;
        };
    }

    public static float of(ItemStack Stack) {
        GunKind Kind = GunItem.kindOf(Stack);
        GunTimer Reload = GunItem.getReload(Stack);
        if (Kind == null || Reload == null) {
            return 0.0F;
        }
        return stage(Kind, Reload.progress(GunPose.gameTime(), 0.0F));
    }

    public static float braced(ItemStack Stack, @Nullable LivingEntity Bearer) {
        return Bearer != null && Bearer.isCrouching() && Bearer.getMainHandItem() == Stack ? 1.0F : 0.0F;
    }
}
