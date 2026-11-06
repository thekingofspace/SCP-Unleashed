package net.rai.scp_unleashed.flamethrower;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.AmmoItem;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;

public final class FlamerRegistry {
    public static final String CanisterName = "fuel_canister";
    public static final int CanisterStack = 16;
    public static final float StreamRange = 40.0F;
    public static final float PilotRange = 12.0F;
    public static final float ToolRange = 16.0F;

    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CanisterFuel = ComponentRegister.registerComponentType("fuel_canister_fuel",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<GunItem> Flamethrower = ItemRegister.register("flamethrower", () -> new FlamethrowerItem(new Item.Properties().stacksTo(1)
            .rarity(Rarity.UNCOMMON).component(GunsRegistry.LoadedAmmo.get(), 0)));
    public static final DeferredItem<AmmoItem> FuelCanister = ItemRegister.register(CanisterName, () -> new FuelCanisterItem(new Item.Properties().stacksTo(CanisterStack)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Ignite = fixedSound("flamethrower_ignite", StreamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Whoosh = fixedSound("flamethrower_whoosh", StreamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = fixedSound("flamethrower_hiss", PilotRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = fixedSound("flamethrower_roar", StreamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = fixedSound("flamethrower_crackle", StreamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = fixedSound("flamethrower_burst", StreamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Pilot = fixedSound("flamethrower_pilot", PilotRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Sputter = fixedSound("flamethrower_sputter", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Pop = fixedSound("flamethrower_pop", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Douse = fixedSound("flamethrower_douse", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> CapOpen = fixedSound("flamethrower_cap_open", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Glug = fixedSound("flamethrower_glug", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> CapClose = fixedSound("flamethrower_cap_close", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = fixedSound("flamethrower_click", ToolRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = fixedSound("flamethrower_sizzle", ToolRange);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flame = ParticleRegister.register("flamethrower_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("flamethrower_ember", () -> new SimpleParticleType(false));

    public static final TagKey<EntityType<?>> FlameVulnerable = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flame_vulnerable"));
    public static final TagKey<Block> FlameBurnableFlesh = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flame_burnable_flesh"));
    public static final ResourceKey<DamageType> StreamDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flamethrower"));
    public static final ResourceKey<DamageType> BurnDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flamethrower_burn"));

    static {
        GunsRegistry.adoptGun(GunKind.Flamethrower, Flamethrower);
        GunsRegistry.adoptAmmo(CanisterName, FuelCanister);
    }

    private FlamerRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
