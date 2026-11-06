package net.rai.scp_unleashed.advmaterials.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlock;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlockEntity;
import net.rai.scp_unleashed.advmaterials.BenchStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum AdvancedMaterialsBenchProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "advanced_materials_bench");
    private static final String DataKey = "AdvancedMaterialsBench";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        AdvancedMaterialsBenchBlockEntity Machine = AdvancedMaterialsBenchBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        ItemStack Result = Machine.getCurrentResult();
        if (!Result.isEmpty()) {
            Info.putString("Result", BuiltInRegistries.ITEM.getKey(Result.getItem()).toString());
            Info.putInt("Count", Result.getCount());
            Info.putInt("Progress", Math.round(Machine.getProgressFraction() * 100.0f));
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        BenchStatus Status = BenchStatus.byName(Info.getString("Status"));
        if (Info.contains("Result")) {
            Item Made = BuiltInRegistries.ITEM.get(ResourceLocation.parse(Info.getString("Result")));
            if (Made != Items.AIR) {
                Component Name = new ItemStack(Made, Math.max(1, Info.getInt("Count"))).getHoverName();
                Tooltip.add(Component.translatable("jade.scp_unleashed.advanced_materials_bench.recipe", Info.getInt("Count"), Name, Info.getInt("Progress")));
            }
        }
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
    }
}
