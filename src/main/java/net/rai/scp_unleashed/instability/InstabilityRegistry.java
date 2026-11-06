package net.rai.scp_unleashed.instability;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

public final class InstabilityRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<InstabilityReactorBlock> Reactor = BlockRegister.register("instability_reactor", () -> new InstabilityReactorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 30.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
            .lightLevel(InstabilityReactorBlock::lightOf)));
    public static final DeferredItem<BlockItem> ReactorItem = ItemRegister.register("instability_reactor",
            () -> new BlockItem(Reactor.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstabilityReactorBlockEntity>> ReactorEntity = BlockEntityTypeRegister.register("instability_reactor",
            () -> BlockEntityType.Builder.of(InstabilityReactorBlockEntity::new, Reactor.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<InstabilityMenu>> ReactorMenuType = MenuTypeRegister.register("instability_reactor",
            () -> new MenuType<>(InstabilityMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("instability_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("instability_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ignite = sound("instability_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shutdown = sound("instability_shutdown");

    private InstabilityRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
