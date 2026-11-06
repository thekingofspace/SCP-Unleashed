package net.rai.scp_unleashed.microhid;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;

public final class MicroHidRegistry {
    public static final float SpinRange = 32.0F;
    public static final float BeamRange = 48.0F;
    public static final float PlatformRange = 16.0F;

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Energy = ComponentRegister.registerComponentType("micro_hid_energy",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<GunItem> MicroHid = ItemRegister.register("micro_hid", () -> new MicroHidItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            .component(Energy.get(), MicroHidEnergy.Capacity)));

    public static final DeferredBlock<MicroChargingPlatformBlock> Platform = BlockRegister.register("micro_charging_platform", () -> new MicroChargingPlatformBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(State -> State.getValue(MicroChargingPlatformBlock.Status) == MicroChargingStatus.Charging ? MicroChargingPlatformBlock.ChargingLight : 0)));
    public static final DeferredItem<BlockItem> PlatformItem = ItemRegister.registerSimpleBlockItem("micro_charging_platform", Platform);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicroChargingPlatformBlockEntity>> PlatformEntity = BlockEntityRegister.register(
            "micro_charging_platform", () -> BlockEntityType.Builder.of(MicroChargingPlatformBlockEntity::new, Platform.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> SpinWhine = fixedSound("micro_hid_spin_whine", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> SpinCharge = fixedSound("micro_hid_spin_charge", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> SpinPower = fixedSound("micro_hid_spin_power", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Ignite = fixedSound("micro_hid_ignite", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> LoopDrone = fixedSound("micro_hid_loop_drone", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Ring = fixedSound("micro_hid_ring", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeUp = fixedSound("micro_hid_charge_up", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Zap = fixedSound("micro_hid_zap", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> ArcLoop = fixedSound("micro_hid_arc_loop", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> ArcCrackle = fixedSound("micro_hid_arc_crackle", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerDown = fixedSound("micro_hid_power_down", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Sputter = fixedSound("micro_hid_sputter", PlatformRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Spark = fixedSound("micro_hid_spark", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> WindDown = fixedSound("micro_hid_wind_down", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> WindDeplete = fixedSound("micro_hid_wind_deplete", SpinRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Dock = fixedSound("micro_charging_platform_dock", PlatformRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Undock = fixedSound("micro_charging_platform_undock", PlatformRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PlatformHum = fixedSound("micro_charging_platform_hum", PlatformRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PlatformFull = fixedSound("micro_charging_platform_full", PlatformRange);

    public static final ResourceKey<DamageType> MicroHidDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "micro_hid"));

    static {
        GunsRegistry.adoptGun(GunKind.MicroHid, MicroHid);
    }

    private MicroHidRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
