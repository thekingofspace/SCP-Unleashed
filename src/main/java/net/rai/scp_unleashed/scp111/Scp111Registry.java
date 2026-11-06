package net.rai.scp_unleashed.scp111;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageBehaviours;

import java.util.List;

public final class Scp111Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_111");
    public static final TagKey<Item> Greens = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_greens"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = sound("scp111_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbientCrackle = sound("scp111_ambient_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Puff = sound("scp111_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> PuffCrackle = sound("scp111_puff_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp111_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Jet = sound("scp111_jet");
    public static final DeferredHolder<SoundEvent, SoundEvent> JetCrackle = sound("scp111_jet_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp111_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp111_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp111_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatCrunch = sound("scp111_eat_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Nibble = sound("scp111_nibble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lay = sound("scp111_lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> LayPlop = sound("scp111_lay_plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hatch = sound("scp111_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> HatchChirp = sound("scp111_hatch_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> EggCrack = sound("scp111_egg_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> EggBreak = sound("scp111_egg_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> EggPick = sound("scp111_egg_pick");
    public static final DeferredHolder<SoundEvent, SoundEvent> EggPlace = sound("scp111_egg_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unbox = sound("scp111_unbox");
    public static final DeferredHolder<SoundEvent, SoundEvent> UnboxJingle = sound("scp111_unbox_jingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> UnboxRustle = sound("scp111_unbox_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spill = sound("scp111_spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpillChirps = sound("scp111_spill_chirps");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stir = sound("scp111_stir");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AmbientLayers = List.of(Ambient, AmbientCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PuffLayers = List.of(Puff, PuffCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HissLayers = List.of(Hiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> JetLayers = List.of(Jet, JetCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NibbleLayers = List.of(Nibble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LayLayers = List.of(Lay, LayPlop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HatchLayers = List.of(Hatch, HatchChirp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> UnboxLayers = List.of(Unbox, UnboxJingle, UnboxRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpillLayers = List.of(Spill, SpillChirps);

    public static final DeferredSoundType EggSounds = new DeferredSoundType(1.0f, 1.0f, EggBreak, EggCrack, EggPlace, EggCrack, EggCrack);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp111Egg>> EggData = ComponentRegister.registerComponentType("scp111_egg",
            Builder -> Builder.persistent(Scp111Egg.EggCodec).networkSynchronized(Scp111Egg.EggStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp111Clutch>> ClutchData = ComponentRegister.registerComponentType("scp111_clutch",
            Builder -> Builder.persistent(Scp111Clutch.ClutchCodec).networkSynchronized(Scp111Clutch.ClutchStreamCodec));

    public static final DeferredHolder<ParticleType<?>, ParticleType<Scp111Breath>> Breath = ParticleRegister.register("scp111_breath", () -> new ParticleType<Scp111Breath>(false) {
        @Override
        public MapCodec<Scp111Breath> codec() {
            return Scp111Breath.BreathCodec;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, Scp111Breath> streamCodec() {
            return Scp111Breath.BreathStreamCodec;
        }
    });

    public static final DeferredHolder<EntityType<?>, EntityType<Scp111Snail>> Snail = EntityTypeRegister.register("scp111_snail",
            () -> EntityType.Builder.<Scp111Snail>of(Scp111Snail::new, MobCategory.CREATURE)
                    .sized(Scp111Snail.Width, Scp111Snail.Height)
                    .eyeHeight(Scp111Snail.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("scp111_snail"));

    public static final DeferredBlock<Scp111EggBlock> EggBlock = BlockRegister.register("scp111_egg",
            () -> new Scp111EggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.3f).sound(EggSounds)
                    .noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<Scp111EggItem> EggItem = ItemRegister.register("scp111_egg",
            () -> new Scp111EggItem(EggBlock.get(), new Item.Properties().stacksTo(Scp111EggItem.StackSize)));
    public static final DeferredItem<Scp111BoxItem> BoxItem = ItemRegister.register("scp111_box",
            () -> new Scp111BoxItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> SnailEgg = ItemRegister.register("scp111_snail_spawn_egg",
            () -> new DeferredSpawnEggItem(Snail, 0xF2E4C6, 0xFF8A2A, new Item.Properties()));

    private Scp111Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        StorageBehaviours.register(Scp111Shelf::handles, Scp111Shelf.Instance);
    }
}
