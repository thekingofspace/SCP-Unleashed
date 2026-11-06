package net.rai.scp_unleashed.mining.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mining.MinerRules;
import net.rai.scp_unleashed.mining.MiningRegistry;
import net.rai.scp_unleashed.mining.PointBankBlockEntity;

import java.util.Locale;

@JeiPlugin
public class MiningJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mining_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(MiningRegistry.PointMinerItem.get()),
                Component.translatable(Prefix + "point_miner.1"),
                Component.translatable(Prefix + "point_miner.2", MinerRules.DrawPerMiner),
                Component.translatable(Prefix + "point_miner.3", MinerRules.waterPerMinute()),
                Component.translatable(Prefix + "point_miner.4", String.format(Locale.ROOT, "%.0f", MinerRules.pointsPerMinute(1.0)), MinerRules.BufferSize));
        Registration.addItemStackInfo(new ItemStack(MiningRegistry.PointBankItem.get()),
                Component.translatable(Prefix + "point_bank.1"),
                Component.translatable(Prefix + "point_bank.2", PointBankBlockEntity.Draw),
                Component.translatable(Prefix + "point_bank.3"));
    }
}
