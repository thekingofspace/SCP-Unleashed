package net.rai.scp_unleashed.scp107;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

public final class Scp107Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_107");
    public static final ResourceKey<DamageType> LavaRainDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp107_lava_rain"));
    public static final ResourceKey<DamageType> CausticRainDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp107_caustic_rain"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp107_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourSlosh = sound("scp107_pour_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourLava = sound("scp107_pour_lava");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourLavaPop = sound("scp107_pour_lava_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourHiss = sound("scp107_pour_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drink = sound("scp107_drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thunder = sound("scp107_thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThunderRoll = sound("scp107_thunder_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> LavaRain = sound("scp107_lava_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = sound("scp107_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SizzlePop = sound("scp107_sizzle_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CausticRain = sound("scp107_caustic_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp107_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burn = sound("scp107_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurnCrackle = sound("scp107_burn_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Corrode = sound("scp107_corrode");
    public static final DeferredHolder<SoundEvent, SoundEvent> CorrodeBubble = sound("scp107_corrode_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> MilkRain = sound("scp107_milk_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> MilkDrip = sound("scp107_milk_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cleanse = sound("scp107_cleanse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CleanseWash = sound("scp107_cleanse_wash");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourOil = sound("scp107_pour_oil");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourOilGlug = sound("scp107_pour_oil_glug");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilRain = sound("scp107_oil_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilDrip = sound("scp107_oil_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Oiled = sound("scp107_oiled");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("scp107_ember", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CausticSplash = ParticleRegister.register("scp107_caustic", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MilkSplash = ParticleRegister.register("scp107_milk", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OilSplash = ParticleRegister.register("scp107_oil", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp107ShellBlock> Shell = BlockRegister.register("scp107_shell", () -> new Scp107ShellBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0f, 6.0f).sound(SoundType.BONE_BLOCK).noOcclusion()
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> ShellItem = ItemRegister.register("scp107_shell",
            () -> new BlockItem(Shell.get(), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp107ShellBlockEntity>> ShellEntity = BlockEntityTypeRegister.register("scp107_shell",
            () -> BlockEntityType.Builder.of(Scp107ShellBlockEntity::new, Shell.get()).build(null));

    private Scp107Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
