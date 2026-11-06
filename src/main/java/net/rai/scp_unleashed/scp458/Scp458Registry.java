package net.rai.scp_unleashed.scp458;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp458Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_458");
    public static final int SliceStack = 16;
    public static final int SliceNutrition = 5;
    public static final float SliceSaturation = 0.6f;

    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp458_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpenFlap = sound("scp458_lid_open_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClose = sound("scp458_lid_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidCloseThump = sound("scp458_lid_close_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidRest = sound("scp458_lid_rest");
    public static final DeferredHolder<SoundEvent, SoundEvent> Take = sound("scp458_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> TakeStretch = sound("scp458_take_stretch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Munch = sound("scp458_munch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MunchCrust = sound("scp458_munch_crust");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxBreak = sound("scp458_box_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxStep = sound("scp458_box_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxPlace = sound("scp458_box_place");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(LidOpen, LidOpenFlap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(LidClose, LidCloseThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(Take, TakeStretch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RestLayers = List.of(LidRest);

    public static final DeferredSoundType Cardboard = new DeferredSoundType(0.8f, 1.0f, BoxBreak, BoxStep, BoxPlace, BoxStep, BoxStep);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp458Contents>> Contents = ComponentRegister.registerComponentType("scp458_box",
            Builder -> Builder.persistent(Scp458Contents.ContentsCodec).networkSynchronized(Scp458Contents.ContentsStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp458Pizza>> Pizza = ComponentRegister.registerComponentType("scp458_pizza",
            Builder -> Builder.persistent(Scp458Pizza.PizzaCodec).networkSynchronized(Scp458Pizza.PizzaStreamCodec));

    public static final DeferredBlock<Scp458BoxBlock> Box = BlockRegister.register("scp458_box",
            () -> new Scp458BoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.GUITAR).strength(0.6f, 1200.0f)
                    .sound(Cardboard).noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp458BoxItem> BoxItem = ItemRegister.register("scp458_box",
            () -> new Scp458BoxItem(Box.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp458BoxBlockEntity>> BoxEntity = BlockEntityTypeRegister.register("scp458_box",
            () -> BlockEntityType.Builder.of(Scp458BoxBlockEntity::new, Box.get()).build(null));

    public static final DeferredItem<Scp458SliceItem> Slice = ItemRegister.register("scp458_slice",
            () -> new Scp458SliceItem(new Item.Properties().stacksTo(SliceStack).rarity(Rarity.UNCOMMON)
                    .food(new FoodProperties.Builder().nutrition(SliceNutrition).saturationModifier(SliceSaturation).build())
                    .component(Pizza.get(), Scp458Pizza.Cheese)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp458Pizza>> Favourite = AttachmentRegister.register("scp458_favourite",
            () -> AttachmentType.builder(() -> Scp458Pizza.Cheese).serialize(Scp458Pizza.PizzaCodec).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp458Pizza>> BodyFavourite = AttachmentRegister.register("scp458_body_favourite",
            () -> AttachmentType.builder(() -> Scp458Pizza.Cheese).serialize(Scp458Pizza.PizzaCodec).build());

    private Scp458Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
