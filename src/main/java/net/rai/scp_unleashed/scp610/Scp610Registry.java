package net.rai.scp_unleashed.scp610;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.faction.FactionRegistry;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp610Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SporeParticle = ParticleRegister.register("scp610_spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MawParticle = ParticleRegister.register("scp610_maw", () -> new SimpleParticleType(true));
    private static final Map<Scp610Form, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Scp610Creature>>> Types = new EnumMap<>(Scp610Form.class);
    private static final List<DeferredItem<DeferredSpawnEggItem>> Eggs = new ArrayList<>();

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_610");
    public static final ResourceKey<DamageType> Consumed = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610"));
    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_immune"));
    public static final TagKey<Block> FleshTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_flesh"));
    public static final TagKey<Block> Unspreadable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_unspreadable"));
    public static final Scp610Organism Organism = new Scp610Organism();

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Infection = AttachmentRegister.register("scp610_infection",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved >= 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp610Trace>> Trace = AttachmentRegister.register("scp610_trace",
            () -> AttachmentType.builder(() -> new Scp610Trace()).build());

    public static final SoundType FleshSound = new DeferredSoundType(1.0f, 1.0f, Scp610Sounds.FleshBreak, Scp610Sounds.FleshStep, Scp610Sounds.FleshPlace,
            Scp610Sounds.FleshHit, Scp610Sounds.FleshFall);

    public static final DeferredBlock<Scp610FleshBlock> Flesh = BlockRegister.register("scp610_flesh", () -> new Scp610FleshBlock(flesh()));
    public static final DeferredBlock<Scp610FleshBlock> VeinedFlesh = BlockRegister.register("scp610_veined_flesh", () -> new Scp610FleshBlock(flesh()));
    public static final DeferredBlock<Scp610FleshBlock> Wound = BlockRegister.register("scp610_wound", () -> new Scp610FleshBlock(flesh()));
    public static final DeferredBlock<Scp610PustuleBlock> Pustule = BlockRegister.register("scp610_pustule", () -> new Scp610PustuleBlock(flesh().lightLevel(State -> 4)));
    public static final DeferredBlock<Scp610SinewBlock> Sinew = BlockRegister.register("scp610_sinew", () -> new Scp610SinewBlock(flesh()));
    public static final DeferredBlock<Scp610TendrilsBlock> Tendrils = BlockRegister.register("scp610_tendrils", () -> new Scp610TendrilsBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_RED).noCollission().instabreak().sound(FleshSound).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<PheromoneCommunicatorBlock> Communicator = BlockRegister.register("pheromone_communicator", () -> new PheromoneCommunicatorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<BlockItem> FleshItem = ItemRegister.registerSimpleBlockItem("scp610_flesh", Flesh);
    public static final DeferredItem<BlockItem> VeinedFleshItem = ItemRegister.registerSimpleBlockItem("scp610_veined_flesh", VeinedFlesh);
    public static final DeferredItem<BlockItem> WoundItem = ItemRegister.registerSimpleBlockItem("scp610_wound", Wound);
    public static final DeferredItem<BlockItem> PustuleItem = ItemRegister.registerSimpleBlockItem("scp610_pustule", Pustule);
    public static final DeferredItem<BlockItem> SinewItem = ItemRegister.registerSimpleBlockItem("scp610_sinew", Sinew);
    public static final DeferredItem<BlockItem> TendrilsItem = ItemRegister.registerSimpleBlockItem("scp610_tendrils", Tendrils);
    public static final DeferredItem<PheromoneCommunicatorItem> CommunicatorItem = ItemRegister.register("pheromone_communicator",
            () -> new PheromoneCommunicatorItem(Communicator.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PheromoneCommunicatorBlockEntity>> CommunicatorEntity = BlockEntityTypeRegister.register(
            "pheromone_communicator", () -> BlockEntityType.Builder.of(PheromoneCommunicatorBlockEntity::new, Communicator.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Apostle>> ApostleType = entity(Scp610Form.Apostle, Scp610Apostle::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Spewer>> SpewerType = entity(Scp610Form.Spewer, Scp610Spewer::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> VillagerType = unit(Scp610Form.Villager);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> PigType = unit(Scp610Form.Pig);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> CowType = unit(Scp610Form.Cow);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> SheepType = unit(Scp610Form.Sheep);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> ChickenType = unit(Scp610Form.Chicken);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> HorseType = unit(Scp610Form.Horse);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> CrawlerType = unit(Scp610Form.Crawler);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> ShamblerType = unit(Scp610Form.Shambler);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> HulkType = unit(Scp610Form.Hulk);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> AmalgamType = unit(Scp610Form.Amalgam);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> ColossusType = unit(Scp610Form.Colossus);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Herald>> HeraldType = entity(Scp610Form.Herald, Scp610Herald::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610TendrilWorm>> TendrilWormType = entity(Scp610Form.TendrilWorm, Scp610TendrilWorm::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610HiveLord>> HiveLordType = entity(Scp610Form.HiveLord, Scp610HiveLord::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Tender>> TenderType = entity(Scp610Form.Tender, Scp610Tender::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Weaver>> WeaverType = entity(Scp610Form.Weaver, Scp610Weaver::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Harvester>> HarvesterType = entity(Scp610Form.Harvester, Scp610Harvester::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Burrower>> BurrowerType = entity(Scp610Form.Burrower, Scp610Burrower::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610SporeBearer>> SporeBearerType = entity(Scp610Form.SporeBearer, Scp610SporeBearer::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610BroodSac>> BroodSacType = entity(Scp610Form.BroodSac, Scp610BroodSac::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610SiegeBrute>> SiegeBruteType = entity(Scp610Form.SiegeBrute, Scp610SiegeBrute::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Stalker>> StalkerType = entity(Scp610Form.Stalker, Scp610Stalker::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Spitter>> SpitterType = entity(Scp610Form.Spitter, Scp610Spitter::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Matriarch>> MatriarchType = entity(Scp610Form.Matriarch, Scp610Matriarch::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp610Bile>> BileType = EntityTypeRegister.register("scp610_bile", () -> EntityType.Builder.<Scp610Bile>of(
            Scp610Bile::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(5).build("scp610_bile"));

    public static final DeferredItem<DeferredSpawnEggItem> ApostleEgg = egg(Scp610Form.Apostle, 0x3A1918, 0x786E5E);
    public static final DeferredItem<DeferredSpawnEggItem> SpewerEgg = egg(Scp610Form.Spewer, 0x3A1918, 0x6A2A2E);
    public static final DeferredItem<DeferredSpawnEggItem> VillagerEgg = egg(Scp610Form.Villager, 0x3A1918, 0x874A44);
    public static final DeferredItem<DeferredSpawnEggItem> PigEgg = egg(Scp610Form.Pig, 0x5A2626, 0x8C4A4C);
    public static final DeferredItem<DeferredSpawnEggItem> CowEgg = egg(Scp610Form.Cow, 0x5A2626, 0xA89D86);
    public static final DeferredItem<DeferredSpawnEggItem> SheepEgg = egg(Scp610Form.Sheep, 0x5A2626, 0x6E3330);
    public static final DeferredItem<DeferredSpawnEggItem> ChickenEgg = egg(Scp610Form.Chicken, 0x5A2626, 0x786E5E);
    public static final DeferredItem<DeferredSpawnEggItem> HorseEgg = egg(Scp610Form.Horse, 0x3A1918, 0x4A1E22);
    public static final DeferredItem<DeferredSpawnEggItem> CrawlerEgg = egg(Scp610Form.Crawler, 0x6E3330, 0x1F0D0D);
    public static final DeferredItem<DeferredSpawnEggItem> ShamblerEgg = egg(Scp610Form.Shambler, 0x6E3330, 0x3A1918);
    public static final DeferredItem<DeferredSpawnEggItem> HulkEgg = egg(Scp610Form.Hulk, 0x6E3330, 0x786E5E);
    public static final DeferredItem<DeferredSpawnEggItem> AmalgamEgg = egg(Scp610Form.Amalgam, 0x1F0D0D, 0x874A44);
    public static final DeferredItem<DeferredSpawnEggItem> ColossusEgg = egg(Scp610Form.Colossus, 0x1F0D0D, 0xA89D86);
    public static final DeferredItem<DeferredSpawnEggItem> HeraldEgg = egg(Scp610Form.Herald, 0x1F0D0D, 0x6A2A2E);
    public static final DeferredItem<DeferredSpawnEggItem> TendrilWormEgg = egg(Scp610Form.TendrilWorm, 0x4A1E22, 0x786E5E);
    public static final DeferredItem<DeferredSpawnEggItem> TenderEgg = egg(Scp610Form.Tender, 0x5A2626, 0x8C4A4C);
    public static final DeferredItem<DeferredSpawnEggItem> WeaverEgg = egg(Scp610Form.Weaver, 0x5A2626, 0x3A1918);
    public static final DeferredItem<DeferredSpawnEggItem> HarvesterEgg = egg(Scp610Form.Harvester, 0x5A2626, 0x4A1E22);
    public static final DeferredItem<DeferredSpawnEggItem> BurrowerEgg = egg(Scp610Form.Burrower, 0x3A1918, 0x6E3330);
    public static final DeferredItem<DeferredSpawnEggItem> SporeBearerEgg = egg(Scp610Form.SporeBearer, 0x3A1918, 0x6A2A2E);
    public static final DeferredItem<DeferredSpawnEggItem> BroodSacEgg = egg(Scp610Form.BroodSac, 0x1F0D0D, 0x6A2A2E);
    public static final DeferredItem<DeferredSpawnEggItem> SiegeBruteEgg = egg(Scp610Form.SiegeBrute, 0x1F0D0D, 0x786E5E);
    public static final DeferredItem<DeferredSpawnEggItem> StalkerEgg = egg(Scp610Form.Stalker, 0x1F0D0D, 0x3A1918);
    public static final DeferredItem<DeferredSpawnEggItem> SpitterEgg = egg(Scp610Form.Spitter, 0x1F0D0D, 0x8C4A4C);
    public static final DeferredItem<DeferredSpawnEggItem> MatriarchEgg = egg(Scp610Form.Matriarch, 0x1F0D0D, 0xA89D86);

    private Scp610Registry() {
    }

    private static BlockBehaviour.Properties flesh() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.9f, 1.5f).sound(FleshSound).randomTicks();
    }

    private static <T extends Scp610Creature> DeferredHolder<EntityType<?>, EntityType<T>> entity(Scp610Form Form, EntityType.EntityFactory<T> Factory) {
        DeferredHolder<EntityType<?>, EntityType<T>> Holder = EntityTypeRegister.register(Form.entityId(), () -> EntityType.Builder.of(Factory, MobCategory.MONSTER)
                .sized(Form.width(), Form.height()).eyeHeight(Form.height() * 0.85f).clientTrackingRange(Form.mobile() ? 10 : 12).build(Form.entityId()));
        Types.put(Form, Holder);
        return Holder;
    }

    private static DeferredHolder<EntityType<?>, EntityType<Scp610Unit>> unit(Scp610Form Form) {
        return entity(Form, (Type, World) -> new Scp610Unit(Type, World, Form));
    }

    private static DeferredItem<DeferredSpawnEggItem> egg(Scp610Form Form, int Base, int Spots) {
        DeferredHolder<EntityType<?>, ? extends EntityType<? extends Scp610Creature>> Holder = Types.get(Form);
        DeferredItem<DeferredSpawnEggItem> Made = ItemRegister.register(Form.entityId() + "_spawn_egg", () -> new DeferredSpawnEggItem(Holder, Base, Spots,
                new Item.Properties().rarity(Form.evolved() || Form == Scp610Form.Spewer || Form == Scp610Form.BroodSac ? Rarity.EPIC : Rarity.RARE)));
        Eggs.add(Made);
        return Made;
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.copyOf(Eggs);
    }

    public static EntityType<? extends Scp610Creature> typeOf(Scp610Form Form) {
        return Types.get(Form).get();
    }

    public static Scp610Organism organism() {
        return Organism;
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        Scp610Sounds.SoundEventRegister.register(ModEventBus);
        FactionRegistry.bootstrap(ModEventBus);
        LabOrganisms.register(Organism);
        Factions.register(Scp610Faction.Instance);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp610Rules::register));
    }
}
