package net.rai.scp_unleashed.assembler.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlock;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlockEntity;
import net.rai.scp_unleashed.assembler.AssemblerStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum AssemblerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "advanced_assembler");
    private static final String DataKey = "Assembler";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        AdvancedAssemblerBlockEntity Assembler = AdvancedAssemblerBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Assembler == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Assembler.getStatus().ordinal());
        ItemStack Result = Assembler.getCurrentResult();
        if (!Result.isEmpty()) {
            Info.putString("Result", BuiltInRegistries.ITEM.getKey(Result.getItem()).toString());
            Info.putInt("Count", Result.getCount());
            Info.putInt("Progress", Math.round(Assembler.getProgressFraction() * 100.0f));
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
        AssemblerStatus Status = AssemblerStatus.byIndex(Info.getByte("Status"));
        if (Info.contains("Result")) {
            Item Made = BuiltInRegistries.ITEM.get(ResourceLocation.parse(Info.getString("Result")));
            if (Made != Items.AIR) {
                Component Name = new ItemStack(Made, Math.max(1, Info.getInt("Count"))).getHoverName();
                Tooltip.add(Component.translatable("jade.scp_unleashed.assembler.recipe", Info.getInt("Count"), Name, Info.getInt("Progress")));
            }
        }
        ChatFormatting Colour = switch (Status) {
            case Working -> ChatFormatting.GOLD;
            case Idle -> ChatFormatting.GREEN;
            case Blocked -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
        String Key = Status == AssemblerStatus.Off && Info.contains("Result") ? "no_power" : Status.getSerializedName();
        Tooltip.add(Component.translatable("gui.scp_unleashed.advanced_assembler.status." + Key).withStyle(Colour));
    }
}
