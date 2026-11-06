package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class Scp427LocketExtensions implements IClientItemExtensions {
    public static final Scp427LocketExtensions Instance = new Scp427LocketExtensions();

    private Scp427LocketExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        return Hand == InteractionHand.MAIN_HAND && Scp427ClientLocket.opening(Holder) ? HumanoidModel.ArmPose.BLOCK : null;
    }
}
