package net.rai.scp_unleashed.food;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FoodRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);

    public static final DeferredBlock<VendingMachineBlock> VendingMachine = BlockRegister.register("vending_machine", () -> new VendingMachineBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(VendingMachineBlock::lightOf)));
    public static final DeferredItem<BlockItem> VendingMachineItem = ItemRegister.registerSimpleBlockItem("vending_machine", VendingMachine);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VendingMachineBlockEntity>> VendingMachineEntity = BlockEntityRegister.register("vending_machine",
            () -> BlockEntityType.Builder.of(VendingMachineBlockEntity::new, VendingMachine.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<VendingMenu>> VendingMenuType = MenuRegister.register("vending_machine",
            () -> new MenuType<>(VendingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private static final Map<Snack, DeferredItem<SnackItem>> Snacks = new EnumMap<>(Snack.class);
    private static final List<DeferredItem<SnackItem>> SnackOrder = new ArrayList<>();

    static {
        for (Snack Product : Snack.values()) {
            DeferredItem<SnackItem> Registered = ItemRegister.register(Product.getName(), () -> new SnackItem(Product, new Item.Properties()));
            Snacks.put(Product, Registered);
            SnackOrder.add(Registered);
        }
    }

    private FoodRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        FoodSounds.SoundEventRegister.register(ModEventBus);
    }

    public static SnackItem snack(Snack Product) {
        return Snacks.get(Product).get();
    }

    public static List<DeferredItem<SnackItem>> snacks() {
        return Collections.unmodifiableList(SnackOrder);
    }
}
