package net.rai.scp_unleashed.stasis.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisRegistry;

@JeiPlugin
public class StasisJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stasis_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (FragmentRarity Grade : FragmentRarity.values()) {
            String Tier = Grade.getSerializedName();
            Component Fragment = Component.translatable("item.scp_unleashed.unstable_fragment_" + Tier);
            Component Next = Grade == FragmentRarity.Abyssal ? Component.translatable("jei.scp_unleashed.info.stasis_pod.top")
                    : Component.translatable("jei.scp_unleashed.info.stasis_pod.upgrade",
                    Component.translatable("item.scp_unleashed.unstable_fragment_" + FragmentRarity.byTier(Grade.getTier() + 1).getSerializedName()));
            Registration.addItemStackInfo(new ItemStack(StasisRegistry.podFor(Grade)),
                    Component.translatable("jei.scp_unleashed.info.stasis_pod.1", Fragment),
                    Component.translatable("jei.scp_unleashed.info.stasis_pod.2", StasisPodItem.clock(StasisPodItem.budget(Grade))),
                    Component.translatable("jei.scp_unleashed.info.stasis_pod.3"),
                    Component.translatable("jei.scp_unleashed.info.stasis_pod.4"),
                    Next);
        }
    }
}
