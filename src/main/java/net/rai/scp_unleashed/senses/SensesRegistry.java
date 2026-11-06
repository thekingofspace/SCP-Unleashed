package net.rai.scp_unleashed.senses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SensesRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);

    public static final String EarplugsId = "earplugs";
    public static final String BlindfoldId = "blindfold";
    public static final String EarsSlot = "ears";
    public static final String HeadSlot = "head";

    public static final DeferredItem<SenseItem> Earplugs = ItemRegister.register(EarplugsId,
            () -> new SenseItem(SenseItem.Kind.Hearing, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SenseItem> Blindfold = ItemRegister.register(BlindfoldId,
            () -> new SenseItem(SenseItem.Kind.Sight, new Item.Properties().stacksTo(1)));

    private SensesRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
    }
}
