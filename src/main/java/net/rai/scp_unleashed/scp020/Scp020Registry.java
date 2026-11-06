package net.rai.scp_unleashed.scp020;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabSurfaces;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.EnumMap;
import java.util.List;

public final class Scp020Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_020");
    public static final int DefaultSpreadLimit = 600;
    public static final int DefaultSpreadChance = 25;
    public static final int GogglesDefense = 1;
    public static final int GogglesEnchantability = 10;
    public static final int GogglesDurabilityFactor = 15;
    public static GameRules.Key<GameRules.IntegerValue> SpreadLimit;
    public static GameRules.Key<GameRules.IntegerValue> SpreadChance;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp020Affliction.Dose>> Affliction = AttachmentRegister.register("scp020_affliction",
            () -> AttachmentType.builder(() -> Scp020Affliction.Dose.None).serialize(Scp020Affliction.Dose.DoseCodec, Saved -> Saved.Intensity() > 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp020Growth>> Growth = AttachmentRegister.register("scp020_growth",
            () -> AttachmentType.builder(Scp020Growth::new).serialize(Scp020Growth.Saver).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp020_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperVoices = sound("scp020_whisper_voices");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperBreath = sound("scp020_whisper_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Plant = sound("scp020_plant");
    public static final DeferredHolder<SoundEvent, SoundEvent> GogglesEquip = sound("scp020_goggles_equip");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spore = ParticleRegister.register("scp020_spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Film = ParticleRegister.register("scp020_film", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Hyperspectral = MaterialRegister.register("scp020_hyperspectral", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, GogglesDefense);
        Defense.put(ArmorItem.Type.CHESTPLATE, 0);
        Defense.put(ArmorItem.Type.LEGGINGS, 0);
        Defense.put(ArmorItem.Type.BOOTS, 0);
        Defense.put(ArmorItem.Type.BODY, 0);
        return new ArmorMaterial(Defense, GogglesEnchantability, GogglesEquip, () -> Ingredient.of(Items.IRON_INGOT),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_hyperspectral_goggles"))), 0.0f, 0.0f);
    });

    public static final DeferredBlock<Scp020MoldBlock> Mold = BlockRegister.register("scp020_mold", () -> new Scp020MoldBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).replaceable().noCollission().noOcclusion().instabreak().sound(SoundType.EMPTY).randomTicks().noLootTable().noTerrainParticles()
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> MoldItem = ItemRegister.register("scp020_mold",
            () -> new BlockItem(Mold.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp020GogglesItem> Goggles = ItemRegister.register("scp020_hyperspectral_goggles",
            () -> new Scp020GogglesItem(Hyperspectral, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .durability(ArmorItem.Type.HELMET.getDurability(GogglesDurabilityFactor))));

    private Scp020Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        LabOrganisms.register(new Scp020Organism());
        LabSurfaces.register(new Scp020Surface());
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp020Registry::registerRules));
    }

    private static void registerRules() {
        if (SpreadLimit == null) {
            SpreadLimit = GameRules.register("scp020SpreadLimit", GameRules.Category.UPDATES, GameRules.IntegerValue.create(DefaultSpreadLimit));
        }
        if (SpreadChance == null) {
            SpreadChance = GameRules.register("scp020SpreadChance", GameRules.Category.UPDATES, GameRules.IntegerValue.create(DefaultSpreadChance));
        }
    }

    public static int spreadLimit(GameRules Rules) {
        return rule(Rules, SpreadLimit, DefaultSpreadLimit);
    }

    public static int spreadChance(GameRules Rules) {
        return Math.max(0, Math.min(100, rule(Rules, SpreadChance, DefaultSpreadChance)));
    }

    private static int rule(GameRules Rules, GameRules.Key<GameRules.IntegerValue> Key, int Fallback) {
        if (Key == null) {
            return Fallback;
        }
        GameRules.IntegerValue Rule = Rules.getRule(Key);
        return Rule == null ? Fallback : Rule.get();
    }
}
