package net.rai.scp_unleashed.scp902;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp902Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_902");
    public static final ResourceKey<Level> Wastes = ResourceKey.create(Registries.DIMENSION, location("the_wastes"));
    public static final float TickRange = 8.0f;

    public static final DeferredHolder<SoundEvent, SoundEvent> Tick = fixedSound("scp902_tick", TickRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> TickBody = fixedSound("scp902_tick_body", TickRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp902_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidShut = sound("scp902_lid_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> Latch = sound("scp902_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> RainHeavy = sound("scp902_rain_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> RainSteel = sound("scp902_rain_steel");
    public static final DeferredHolder<SoundEvent, SoundEvent> RainMud = sound("scp902_rain_mud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wind = sound("scp902_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> FarRifle = sound("scp902_far_rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FarCrack = sound("scp902_far_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> FarBurst = sound("scp902_far_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> FarArtillery = sound("scp902_far_artillery");
    public static final DeferredHolder<SoundEvent, SoundEvent> FarRumble = sound("scp902_far_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> DepartSwell = sound("scp902_depart_swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> DepartAir = sound("scp902_depart_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArriveSwell = sound("scp902_arrive_swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReturnSwell = sound("scp902_return_swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> TextTone = sound("scp902_text_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> TextBell = sound("scp902_text_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CopyFade = sound("scp902_copy_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> BulletWhiz = sound("scp902_bullet_whiz");
    public static final DeferredHolder<SoundEvent, SoundEvent> BulletCrack = sound("scp902_bullet_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BulletSnap = sound("scp902_bullet_snap");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(LidOpen, Latch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShutLayers = List.of(LidShut, Latch);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp902Latch>> LatchComponent = ComponentRegister.registerComponentType("scp902_latch",
            Builder -> Builder.persistent(Scp902Latch.LatchCodec).networkSynchronized(Scp902Latch.LatchStreamCodec));

    public static final DeferredBlock<Scp902BoxBlock> Box = BlockRegister.register("scp902_box",
            () -> new Scp902BoxBlock(boxProperties(), false));
    public static final DeferredItem<Scp902BoxItem> BoxItem = ItemRegister.register("scp902_box",
            () -> new Scp902BoxItem(Box.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredBlock<Scp902BoxBlock> TrenchBox = BlockRegister.register("scp902_trench_box",
            () -> new Scp902BoxBlock(boxProperties().strength(-1.0f, 3600000.0f).noLootTable(), true));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp902BoxBlockEntity>> BoxEntity = BlockEntityTypeRegister.register("scp902_box",
            () -> BlockEntityType.Builder.of(Scp902BoxBlockEntity::new, Box.get(), TrenchBox.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp902Copy>> Copy = EntityTypeRegister.register("scp902_copy",
            () -> EntityType.Builder.<Scp902Copy>of(Scp902Copy::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f).fireImmune().noSummon()
                    .clientTrackingRange(10).updateInterval(20).build("scp902_copy"));

    public static final DeferredBlock<Block> TrenchMud = BlockRegister.register("scp902_trench_mud",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.MUD)));
    public static final DeferredItem<BlockItem> TrenchMudItem = ItemRegister.registerSimpleBlockItem("scp902_trench_mud", TrenchMud);

    public static final Map<Scp902Prop, DeferredBlock<Scp902PropBlock>> Props = new EnumMap<>(Scp902Prop.class);
    public static final List<DeferredItem<BlockItem>> PropItems = new ArrayList<>();

    static {
        for (Scp902Prop Kind : Scp902Prop.values()) {
            DeferredBlock<Scp902PropBlock> Made = BlockRegister.register(Kind.id(), () -> new Scp902PropBlock(Kind, BlockBehaviour.Properties.of().mapColor(Kind.colour())
                    .strength(1.0f, 3.0f).sound(Kind.sound()).noOcclusion().isRedstoneConductor((State, Getter, Pos) -> false)
                    .isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
            Props.put(Kind, Made);
            PropItems.add(ItemRegister.registerSimpleBlockItem(Kind.id(), Made));
        }
    }

    private Scp902Registry() {
    }

    private static BlockBehaviour.Properties boxProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(1.2f, 1200.0f)
                .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), Range));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
