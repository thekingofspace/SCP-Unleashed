package net.rai.scp_unleashed.scp179;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp179Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_179");
    public static final ResourceLocation TargetId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp179");

    public static final DeferredBlock<SatelliteTerminalBlock> Terminal = BlockRegister.register("satellite_terminal",
            () -> new SatelliteTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(SatelliteTerminalBlock::lightOf).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    public static final DeferredItem<BlockItem> TerminalItem = ItemRegister.registerSimpleBlockItem("satellite_terminal", Terminal);
    public static final DeferredItem<SpaceCoordinatorItem> Coordinator = ItemRegister.register("space_coordinator",
            () -> new SpaceCoordinatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteTerminalBlockEntity>> TerminalEntity = BlockEntityTypeRegister.register("satellite_terminal",
            () -> BlockEntityType.Builder.of(SatelliteTerminalBlockEntity::new, Terminal.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> CoordinatorTarget = ComponentRegister.registerComponentType("space_coordinator_target",
            Builder -> Builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> TerminalLocations = ComponentRegister.registerComponentType("satellite_terminal_locations",
            Builder -> Builder.persistent(ResourceLocation.CODEC.listOf()).networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<SoundEvent, SoundEvent> Link = sound("satellite_terminal_link");
    public static final DeferredHolder<SoundEvent, SoundEvent> LinkChime = sound("satellite_terminal_link_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tune = sound("satellite_terminal_tune");
    public static final DeferredHolder<SoundEvent, SoundEvent> TuneClick = sound("satellite_terminal_tune_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Omen = sound("scp179_omen");
    public static final DeferredHolder<SoundEvent, SoundEvent> OmenBeware = sound("scp179_omen_beware");
    public static final DeferredHolder<SoundEvent, SoundEvent> OmenFriend = sound("scp179_omen_friend");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LinkLayers = List.of(Link, LinkChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TuneLayers = List.of(Tune, TuneClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BewareLayers = List.of(Omen, OmenBeware);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FriendLayers = List.of(Omen, OmenFriend);

    private Scp179Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        StartConditions.register(Scp179CoordinatorCondition.TypeName, Scp179CoordinatorCondition.ConditionCodec);
        SkyTargets.register(new SkyTarget(TargetId, "SCP-179", "satellite.scp_unleashed.target.scp179", "satellite.scp_unleashed.target.scp179.region"));
    }
}
