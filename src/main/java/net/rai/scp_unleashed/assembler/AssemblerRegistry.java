package net.rai.scp_unleashed.assembler;

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
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class AssemblerRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<AdvancedAssemblerBlock> AdvancedAssembler = BlockRegister.register("advanced_assembler", () -> new AdvancedAssemblerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(AdvancedAssemblerBlock::lightOf)));
    public static final DeferredItem<BlockItem> AdvancedAssemblerItem = ItemRegister.registerSimpleBlockItem("advanced_assembler", AdvancedAssembler);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedAssemblerBlockEntity>> AdvancedAssemblerEntity = BlockEntityRegister.register("advanced_assembler",
            () -> BlockEntityType.Builder.of(AdvancedAssemblerBlockEntity::new, AdvancedAssembler.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> AssemblerMenuType = MenuRegister.register("advanced_assembler",
            () -> new MenuType<>(AssemblerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> AssemblingType = RecipeTypeRegister.register("assembling",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "assembling")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblingRecipe>> AssemblingSerializer = RecipeSerializerRegister.register("assembling",
            AssemblingRecipe.Serializer::new);
    public static final DeferredHolder<SoundEvent, SoundEvent> Servo = sound("assembler_servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> Weld = sound("assembler_weld");
    public static final DeferredHolder<SoundEvent, SoundEvent> Finish = sound("assembler_finish");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spark = ParticleRegister.register("assembler_spark", () -> new SimpleParticleType(false));

    private AssemblerRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
