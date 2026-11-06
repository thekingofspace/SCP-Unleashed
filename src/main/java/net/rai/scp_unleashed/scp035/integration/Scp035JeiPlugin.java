package net.rai.scp_unleashed.scp035.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp035.Scp035Carry;
import net.rai.scp_unleashed.scp035.Scp035Consume;
import net.rai.scp_unleashed.scp035.Scp035Lure;
import net.rai.scp_unleashed.scp035.Scp035MaskItem;

@JeiPlugin
public class Scp035JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp035MaskItem.fresh(),
                Component.translatable("jei.scp_unleashed.info.scp035.1"),
                Component.translatable("jei.scp_unleashed.info.scp035.2", (int) Scp035Lure.PullRange),
                Component.translatable("jei.scp_unleashed.info.scp035.3"),
                Component.translatable("jei.scp_unleashed.info.scp035.fatal", Math.round(Scp035Consume.StrikeDamage / 2.0f)),
                Component.translatable("jei.scp_unleashed.info.scp035.4"),
                Component.translatable("jei.scp_unleashed.info.scp035.5", Scp035Carry.EquipTicks / 20),
                Component.translatable("jei.scp_unleashed.info.scp035.6"));
    }
}
