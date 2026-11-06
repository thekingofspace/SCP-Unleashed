package net.rai.scp_unleashed.scp031.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp031.Scp031Census;
import net.rai.scp_unleashed.scp031.Scp031Mimic;
import net.rai.scp_unleashed.scp031.Scp031Polyp;
import net.rai.scp_unleashed.scp031.Scp031Registry;

@JeiPlugin
public class Scp031JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp031_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp031Registry.MimicEgg.get()),
                Component.translatable(Prefix + "scp031.1", (int) Scp031Mimic.DisguiseRange),
                Component.translatable(Prefix + "scp031.2", (int) Scp031Mimic.NoticeRange, (int) Scp031Mimic.FriendlyRange),
                Component.translatable(Prefix + "scp031.3", Scp031Mimic.GrabRange),
                Component.translatable(Prefix + "scp031.4", (int) Scp031Mimic.HoldBreakDamage),
                Component.translatable(Prefix + "scp031.5"));
        Registration.addItemStackInfo(new ItemStack(Scp031Registry.PolypEgg.get()),
                Component.translatable(Prefix + "scp031_polyp.1"),
                Component.translatable(Prefix + "scp031_polyp.2", String.valueOf(Scp031Polyp.MinGrowth / 1200.0), String.valueOf((Scp031Polyp.MinGrowth + Scp031Polyp.GrowthSpread) / 1200.0), Scp031Polyp.Stages),
                Component.translatable(Prefix + "scp031_polyp.3", Scp031Census.Cap));
    }
}
