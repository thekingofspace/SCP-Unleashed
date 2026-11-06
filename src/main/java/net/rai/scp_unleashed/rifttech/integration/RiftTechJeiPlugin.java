package net.rai.scp_unleashed.rifttech.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rifttech.CollectorTuning;
import net.rai.scp_unleashed.rifttech.GridTuning;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;

@JeiPlugin
public class RiftTechJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rifttech_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(RiftTechRegistry.RiftCollectorItem.get()),
                Component.translatable("jei.scp_unleashed.info.rift_collector.1", CollectorTuning.BaseRange),
                Component.translatable("jei.scp_unleashed.info.rift_collector.2", CollectorTuning.IdleDraw,
                        CollectorTuning.drawFor(FragmentRarity.Faint, false), CollectorTuning.drawFor(FragmentRarity.Abyssal, false)),
                Component.translatable("jei.scp_unleashed.info.rift_collector.3", CollectorTuning.BufferSlots),
                Component.translatable("jei.scp_unleashed.info.rift_collector.4", CollectorTuning.RangePerUpgrade, CollectorTuning.maxRange()));
        Registration.addItemStackInfo(new ItemStack(RiftTechRegistry.InstabilityPylonItem.get()),
                Component.translatable("jei.scp_unleashed.info.instability_pylon.1", GridTuning.MinSide, GridTuning.MaxSide),
                Component.translatable("jei.scp_unleashed.info.instability_pylon.2", GridTuning.MinRise, GridTuning.MaxRise),
                Component.translatable("jei.scp_unleashed.info.instability_pylon.3", GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Faint, false)),
                        GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Abyssal, false)), GridTuning.RectangleCap, GridTuning.BoxCap),
                Component.translatable("jei.scp_unleashed.info.instability_pylon.4"));
        Registration.addItemStackInfo(new ItemStack(RiftTechRegistry.ApocalypticFragment.get()),
                Component.translatable("jei.scp_unleashed.info.apocalyptic_rift_fragment.1"),
                Component.translatable("jei.scp_unleashed.info.apocalyptic_rift_fragment.2"));
        Registration.addItemStackInfo(new ItemStack(RiftTechRegistry.RangeUpgrade.get()),
                Component.translatable("jei.scp_unleashed.info.range_upgrade.1", CollectorTuning.RangePerUpgrade));
    }
}
