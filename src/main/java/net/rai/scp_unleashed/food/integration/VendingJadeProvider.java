package net.rai.scp_unleashed.food.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.food.FoodRegistry;
import net.rai.scp_unleashed.food.Snack;
import net.rai.scp_unleashed.food.VendingMachineBlock;
import net.rai.scp_unleashed.food.VendingMachineBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public enum VendingJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "vending_machine");
    private static final String DataKey = "Vending";
    private static final String Prefix = "jade.scp_unleashed.vending_machine.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        VendingMachineBlockEntity Machine = VendingMachineBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Credit", Machine.getCredit());
        Info.putInt("Vending", Machine.getVending());
        Map<Item, Integer> Waiting = new LinkedHashMap<>();
        IItemHandler Tray = Machine.getTray();
        for (int Slot = 0; Slot < Tray.getSlots(); Slot++) {
            ItemStack Stack = Tray.getStackInSlot(Slot);
            if (!Stack.isEmpty()) {
                Waiting.merge(Stack.getItem(), Stack.getCount(), Integer::sum);
            }
        }
        ListTag Entries = new ListTag();
        for (Map.Entry<Item, Integer> Entry : Waiting.entrySet()) {
            CompoundTag Line = new CompoundTag();
            Line.putString("Id", BuiltInRegistries.ITEM.getKey(Entry.getKey()).toString());
            Line.putInt("Count", Entry.getValue());
            Entries.add(Line);
        }
        Info.put("Tray", Entries);
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        int Credit = Info.getInt("Credit");
        Tooltip.add(Component.translatable(Prefix + "credit", Credit, VendingMachineBlockEntity.MaxCredit).withStyle(Credit > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        Snack Vending = Snack.byIndex(Info.getInt("Vending"));
        if (Vending != null) {
            Tooltip.add(Component.translatable(Prefix + "vending", new ItemStack(FoodRegistry.snack(Vending)).getHoverName()).withStyle(ChatFormatting.YELLOW));
        }
        ListTag Entries = Info.getList("Tray", Tag.TAG_COMPOUND);
        if (Entries.isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "tray_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        MutableComponent Waiting = Component.empty();
        for (int Index = 0; Index < Entries.size(); Index++) {
            CompoundTag Line = Entries.getCompound(Index);
            Item Found = BuiltInRegistries.ITEM.get(ResourceLocation.parse(Line.getString("Id")));
            if (Found == Items.AIR) {
                continue;
            }
            if (!Waiting.getSiblings().isEmpty()) {
                Waiting.append(Component.literal(", "));
            }
            Waiting.append(Component.translatable(Prefix + "tray_entry", new ItemStack(Found).getHoverName(), Line.getInt("Count")));
        }
        Tooltip.add(Component.translatable(Prefix + "tray", Waiting).withStyle(ChatFormatting.GREEN));
    }
}
