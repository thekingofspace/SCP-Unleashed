package net.rai.scp_unleashed.scp006;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp006Registry {
    public static final DeferredRegister<FluidType> FluidTypeRegister = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<Fluid> FluidRegister = DeferredRegister.create(Registries.FLUID, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_006");
    public static final int SlopeFind = 4;
    public static final int DropOff = 1;
    public static final int TickRate = 5;
    public static final float Resistance = 100.0f;

    public static final DeferredHolder<FluidType, FluidType> WaterType = FluidTypeRegister.register("scp006", () -> new Scp006FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.scp_unleashed.scp006")
            .fallDistanceModifier(0.0f)
            .canExtinguish(true)
            .canConvertToSource(true)
            .supportsBoating(true)
            .canHydrate(true)
            .rarity(Rarity.RARE)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> Water = FluidRegister.register("scp006", () -> new Scp006Fluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FlowingWater = FluidRegister.register("scp006_flowing", () -> new Scp006Fluid.Flowing(properties()));
    public static final DeferredBlock<Scp006LiquidBlock> Liquid = BlockRegister.register("scp006", () -> new Scp006LiquidBlock(Water.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER).replaceable().noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));
    public static final DeferredItem<Scp006BucketItem> Bucket = ItemRegister.register("scp006_bucket", () -> new Scp006BucketItem(Water.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Fill = ComponentRegister.registerComponentType("scp006_fill",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<Scp006VesselItem> Vessel = ItemRegister.register("scp006_vessel",
            () -> new Scp006VesselItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(Fill.get(), Scp006VesselItem.Capacity)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp006_glint", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp006_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeBell = sound("scp006_chime_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeRipple = sound("scp006_chime_ripple");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unstop = sound("scp006_vessel_unstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp006_vessel_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crack = sound("scp006_vessel_crack");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(Chime, ChimeBell, ChimeRipple);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PourLayers = List.of(Unstop, Pour);

    private Scp006Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(WaterType, Water, FlowingWater)
                .bucket(Bucket).block(Liquid).slopeFindDistance(SlopeFind).levelDecreasePerBlock(DropOff).tickRate(TickRate).explosionResistance(Resistance);
    }

    public static void register(IEventBus ModEventBus) {
        FluidTypeRegister.register(ModEventBus);
        FluidRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
