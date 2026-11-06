package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import org.jetbrains.annotations.Nullable;

public class GunClientExtensions implements IClientItemExtensions {
    @Nullable
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || Hand != InteractionHand.MAIN_HAND) {
            return null;
        }
        return PlayerGunStance.armPose(Holder, Stack, Kind);
    }
}
