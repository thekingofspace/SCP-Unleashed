package net.rai.scp_unleashed.marksman;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.GunsRegistry;

public final class MarksmanRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Coins = ComponentRegister.registerComponentType("marksman_coins",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunTimer>> CoinCharge = ComponentRegister.registerComponentType("marksman_coin_charge",
            Builder -> Builder.networkSynchronized(GunTimer.TimerStreamCodec));

    public static final DeferredItem<MarksmanRevolverItem> MarksmanRevolver = ItemRegister.register("marksman_revolver",
            () -> new MarksmanRevolverItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(GunsRegistry.LoadedAmmo.get(), 0)
                    .component(Coins.get(), CoinToss.MaxCoins)));

    public static final DeferredHolder<EntityType<?>, EntityType<CoinEntity>> Coin = EntityRegister.register("marksman_coin",
            () -> EntityType.Builder.<CoinEntity>of(CoinEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(2)
                    .fireImmune().noSummon().build("marksman_coin"));

    public static final DeferredHolder<SoundEvent, SoundEvent> CoinFlick = fixedSound("marksman_coin_flick", 24.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> CoinChime = fixedSound("marksman_coin_chime", 24.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> CoinLand = fixedSound("marksman_coin_land", 16.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> RicochetPing = fixedSound("marksman_ricochet_ping", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> RicochetClang = fixedSound("marksman_ricochet_clang", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> RicochetZing = fixedSound("marksman_ricochet_zing", 48.0F);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("marksman_glint", () -> new SimpleParticleType(false));

    private MarksmanRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        EntityRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
