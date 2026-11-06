package net.rai.scp_unleashed.fusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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

public final class FusionRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<FusionReactorBlock> FusionReactor = BlockRegister.register("fusion_reactor", () -> new FusionReactorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 24.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .forceSolidOn().isValidSpawn((State, World, Pos, Kind) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .lightLevel(FusionReactorBlock::lightOf)));
    public static final DeferredItem<BlockItem> FusionReactorItem = ItemRegister.registerSimpleBlockItem("fusion_reactor", FusionReactor);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorBlockEntity>> ReactorEntity = BlockEntityRegister.register("fusion_reactor",
            () -> BlockEntityType.Builder.of(FusionReactorBlockEntity::new, FusionReactor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionPortBlockEntity>> PortEntity = BlockEntityRegister.register("fusion_port",
            () -> BlockEntityType.Builder.of(FusionPortBlockEntity::new, FusionReactor.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<FusionMenu>> ReactorMenu = MenuRegister.register("fusion_reactor",
            () -> new MenuType<>(FusionMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("fusion_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumSub = sound("fusion_hum_sub");
    public static final DeferredHolder<SoundEvent, SoundEvent> Charge = sound("fusion_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ignite = sound("fusion_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> IgniteStrike = sound("fusion_ignite_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shutdown = sound("fusion_shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShutdownLow = sound("fusion_shutdown_low");

    private FusionRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
