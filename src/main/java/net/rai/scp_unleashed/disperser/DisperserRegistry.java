package net.rai.scp_unleashed.disperser;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
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
import net.rai.scp_unleashed.grenade.GrenadeTuning;

import java.util.List;

public final class DisperserRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> Culture = DataComponentRegister.registerComponentType("organic_grenade_culture",
            Builder -> Builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredBlock<DisperserBlock> Disperser = BlockRegister.register("disperser", () -> new DisperserBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .lightLevel(State -> State.getValue(DisperserBlock.Active) ? DisperserTuning.ActiveLight : 0)));
    public static final DeferredItem<BlockItem> DisperserItem = ItemRegister.registerSimpleBlockItem("disperser", Disperser, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisperserBlockEntity>> DisperserEntity = BlockEntityTypeRegister.register("disperser",
            () -> BlockEntityType.Builder.of(DisperserBlockEntity::new, Disperser.get()).build(null));

    public static final DeferredItem<OrganicGrenadeItem> OrganicGrenade = ItemRegister.register("organic_grenade",
            () -> new OrganicGrenadeItem(new Item.Properties().stacksTo(GrenadeTuning.StackSize).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<EntityType<?>, EntityType<OrganicGrenadeEntity>> OrganicGrenadeType = EntityTypeRegister.register("organic_grenade",
            () -> EntityType.Builder.<OrganicGrenadeEntity>of(OrganicGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .eyeHeight(0.125F)
                    .fireImmune()
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("organic_grenade"));
    public static final DeferredHolder<EntityType<?>, EntityType<OrganismCloud>> Cloud = EntityTypeRegister.register("organism_cloud",
            () -> EntityType.Builder.<OrganismCloud>of(OrganismCloud::new, MobCategory.MISC)
                    .sized((float) DisperserTuning.CloudRadius * 2.0F, 1.0F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("organism_cloud"));

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> Haze = ParticleRegister.register("organism_haze",
            () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<OrganicGrenadeRecipe>> LoadingSerializer = RecipeSerializerRegister.register("organic_grenade_loading",
            () -> new SimpleCraftingRecipeSerializer<>(OrganicGrenadeRecipe::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("disperser_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Valve = sound("disperser_valve");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spit = sound("disperser_spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pop = sound("organic_grenade_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Splat = sound("organic_grenade_splat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spray = sound("organic_grenade_spray");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Released = List.of(Hiss, Valve, Spit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Burst = List.of(Pop, Splat, Spray);

    private DisperserRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
