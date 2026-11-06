package net.rai.scp_unleashed.scp163;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp163Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final float FarSoundRange = 256.0f;
    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_163");
    public static final ResourceKey<DamageType> Asphyxiation = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_asphyxiation"));
    public static final TagKey<Item> Treats = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_treats"));
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));
    public static final BlockSetType HullSet = BlockSetType.register(new BlockSetType("scp_unleashed_scp163_hull", true, false, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chitter = sound("scp163_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChitterClick = sound("scp163_chitter_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Approve = sound("scp163_approve");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApprovePatter = sound("scp163_approve_patter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Disapprove = sound("scp163_disapprove");
    public static final DeferredHolder<SoundEvent, SoundEvent> DisapproveThump = sound("scp163_disapprove_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gasp = sound("scp163_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GaspWheeze = sound("scp163_gasp_wheeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breathe = sound("scp163_breathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp163_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepTap = sound("scp163_step_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp163_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp163_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitOff = sound("scp163_suit_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitOn = sound("scp163_suit_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitSeal = sound("scp163_suit_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp163_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApparatusHum = sound("scp163_apparatus_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApparatusDraw = sound("scp163_apparatus_draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleChirp = sound("scp163_console_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleKeys = sound("scp163_console_keys");
    public static final DeferredHolder<SoundEvent, SoundEvent> SignalPulse = sound("scp163_signal_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SignalChime = sound("scp163_signal_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> HatchHiss = sound("scp163_hatch_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> CraftRush = sound("scp163_craft_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> CraftWhine = sound("scp163_craft_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> CraftBurn = sound("scp163_craft_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrashBoom = farSound("scp163_crash_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrashMetal = farSound("scp163_crash_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrashRumble = farSound("scp163_crash_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Debris = farSound("scp163_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> HullTick = sound("scp163_hull_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clap = sound("scp163_clap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClapPat = sound("scp163_clap_pat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drum = sound("scp163_drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrumThud = sound("scp163_drum_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ripple = sound("scp163_ripple");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stretch = sound("scp163_stretch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Preen = sound("scp163_preen");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tap = sound("scp163_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapClick = sound("scp163_tap_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp163_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SettleChime = sound("scp163_settle_chime");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChitterLayers = List.of(Chitter, ChitterClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ApproveLayers = List.of(Approve, ApprovePatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DisapproveLayers = List.of(Disapprove, DisapproveThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GaspLayers = List.of(Gasp, GaspWheeze);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SuitOffLayers = List.of(SuitOff, SuitSeal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SuitOnLayers = List.of(SuitOn, SuitSeal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ApparatusLayers = List.of(ApparatusHum, ApparatusDraw);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ConsoleLayers = List.of(ConsoleChirp, ConsoleKeys);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SignalLayers = List.of(SignalPulse, SignalChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrashLayers = List.of(CrashBoom, CrashMetal, CrashRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClapLayers = List.of(Clap, ClapPat);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DrumLayers = List.of(Drum, DrumThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RippleLayers = List.of(Ripple);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StretchLayers = List.of(Stretch, StepTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PreenLayers = List.of(Preen);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BobLayers = List.of(Step, StepTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TapLayers = List.of(Tap, TapClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SettleLayers = List.of(Settle, SettleChime);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Breath = ParticleRegister.register("scp163_breath", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp163HullBlock> Hull = BlockRegister.register("scp163_hull", () -> new Scp163HullBlock(hull(0)));
    public static final DeferredBlock<Scp163HullBlock> HullSeam = BlockRegister.register("scp163_hull_seam", () -> new Scp163HullBlock(hull(0)));
    public static final DeferredBlock<Scp163HullBlock> HullLight = BlockRegister.register("scp163_hull_light", () -> new Scp163HullBlock(hull(Scp163HullBlock.LightLevel)));
    public static final DeferredBlock<Scp163HullBlock> Deck = BlockRegister.register("scp163_deck", () -> new Scp163HullBlock(hull(0)));
    public static final DeferredBlock<Scp163HatchBlock> Hatch = BlockRegister.register("scp163_hatch", () -> new Scp163HatchBlock(HullSet, BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(Scp163HullBlock.Hardness, Scp163HullBlock.Resistance).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp163ApparatusBlock> Apparatus = BlockRegister.register("scp163_apparatus", () -> new Scp163ApparatusBlock(device()
            .lightLevel(State -> State.getValue(Scp163ApparatusBlock.Supplying) ? Scp163ApparatusBlock.ActiveLight : Scp163ApparatusBlock.IdleLight)));
    public static final DeferredBlock<Scp163DeviceBlock> Console = BlockRegister.register("scp163_console", () -> new Scp163DeviceBlock(device()
            .lightLevel(State -> State.getValue(Scp163DeviceBlock.Busy) ? 7 : 3), Scp163DeviceBlock.Kind.Console));
    public static final DeferredBlock<Scp163DeviceBlock> Signaller = BlockRegister.register("scp163_signaller", () -> new Scp163DeviceBlock(device()
            .lightLevel(State -> State.getValue(Scp163DeviceBlock.Busy) ? 10 : 4), Scp163DeviceBlock.Kind.Signaller));

    public static final DeferredItem<BlockItem> HullItem = ItemRegister.registerSimpleBlockItem("scp163_hull", Hull);
    public static final DeferredItem<BlockItem> HullSeamItem = ItemRegister.registerSimpleBlockItem("scp163_hull_seam", HullSeam);
    public static final DeferredItem<BlockItem> HullLightItem = ItemRegister.registerSimpleBlockItem("scp163_hull_light", HullLight);
    public static final DeferredItem<BlockItem> DeckItem = ItemRegister.registerSimpleBlockItem("scp163_deck", Deck);
    public static final DeferredItem<DoubleHighBlockItem> HatchItem = ItemRegister.register("scp163_hatch", () -> new DoubleHighBlockItem(Hatch.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ApparatusItem = ItemRegister.register("scp163_apparatus",
            () -> new BlockItem(Apparatus.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<BlockItem> ConsoleItem = ItemRegister.register("scp163_console",
            () -> new BlockItem(Console.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> SignallerItem = ItemRegister.register("scp163_signaller",
            () -> new BlockItem(Signaller.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp163SuitItem> Suit = ItemRegister.register("scp163_suit",
            () -> new Scp163SuitItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp163ApparatusBlockEntity>> ApparatusEntity = BlockEntityTypeRegister.register("scp163_apparatus",
            () -> BlockEntityType.Builder.of(Scp163ApparatusBlockEntity::new, Apparatus.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp163Castaway>> Castaway = EntityTypeRegister.register("scp163_castaway",
            () -> EntityType.Builder.<Scp163Castaway>of(Scp163Castaway::new, MobCategory.CREATURE)
                    .sized(Scp163Castaway.Width, Scp163Castaway.Height)
                    .eyeHeight(Scp163Castaway.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp163_castaway"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp163CraftEntity>> Craft = EntityTypeRegister.register("scp163_craft",
            () -> EntityType.Builder.<Scp163CraftEntity>of(Scp163CraftEntity::new, MobCategory.MISC)
                    .sized(2.0f, 1.6f)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(16)
                    .updateInterval(10)
                    .build("scp163_craft"));

    public static final DeferredItem<DeferredSpawnEggItem> CastawayEgg = ItemRegister.register("scp163_castaway_spawn_egg",
            () -> new DeferredSpawnEggItem(Castaway, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    private Scp163Registry() {
    }

    private static BlockBehaviour.Properties hull(int Light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
                .strength(Scp163HullBlock.Hardness, Scp163HullBlock.Resistance).sound(SoundType.NETHERITE_BLOCK).lightLevel(State -> Light);
    }

    private static BlockBehaviour.Properties device() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.2f, 6.0f).sound(SoundType.COPPER_GRATE).noOcclusion().pushReaction(PushReaction.BLOCK);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> farSound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), FarSoundRange));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp163CrashCondition.TypeName, Scp163CrashCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp163Crashes::spawn);
        ReaderTargets.onHandoff((Server, CaseId, From, To) -> Scp163Crashes.get(Server).caseMoved(CaseId, To));
    }
}
