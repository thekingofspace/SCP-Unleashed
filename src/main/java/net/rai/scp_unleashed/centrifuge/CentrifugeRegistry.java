package net.rai.scp_unleashed.centrifuge;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

public final class CentrifugeRegistry {
    public static final ResourceLocation CentrifugingId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "centrifuging");

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<CentrifugeBlock> Centrifuge = BlockRegister.register("centrifuge", () -> new CentrifugeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .lightLevel(State -> State.getValue(CentrifugeBlock.Active) ? CentrifugeBlock.ActiveLight : 0)));
    public static final DeferredItem<BlockItem> CentrifugeItem = ItemRegister.registerSimpleBlockItem("centrifuge", Centrifuge);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CentrifugeEntity = BlockEntityTypeRegister.register("centrifuge",
            () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, Centrifuge.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CentrifugeMenuType = MenuTypeRegister.register("centrifuge",
            () -> new MenuType<>(CentrifugeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> Centrifuging = RecipeTypeRegister.register("centrifuging",
            () -> RecipeType.simple(CentrifugingId));
    public static final DeferredHolder<RecipeSerializer<?>, CentrifugeRecipe.Serializer> CentrifugingSerializer = RecipeSerializerRegister.register("centrifuging",
            CentrifugeRecipe.Serializer::new);
    public static final DeferredHolder<SoundEvent, SoundEvent> Whir = SoundEventRegister.register("centrifuge_whir",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "centrifuge_whir")));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Grit = ParticleRegister.register("centrifuge_grit", () -> new SimpleParticleType(false));

    private CentrifugeRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
