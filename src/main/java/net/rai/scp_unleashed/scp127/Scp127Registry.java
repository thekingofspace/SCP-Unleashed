package net.rai.scp_unleashed.scp127;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;

public final class Scp127Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Rank = ComponentRegister.registerComponentType("scp127_level",
            Builder -> Builder.persistent(ExtraCodecs.intRange(1, Scp127Levels.MaxLevel)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Kills = ComponentRegister.registerComponentType("scp127_kills",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> Regrowth = ComponentRegister.registerComponentType("scp127_regrowth",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> Used = ComponentRegister.registerComponentType("scp127_used",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> Fed = ComponentRegister.registerComponentType("scp127_fed",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredItem<GunItem> LivingGun = ItemRegister.register("scp127_living_gun", () -> new Scp127Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            .fireResistant().component(GunsRegistry.LoadedAmmo.get(), GunKind.Scp127.getStats().getCapacity()).component(Rank.get(), 1).component(Kills.get(), 0)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Purr = sound("scp127_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurrBody = sound("scp127_purr_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whine = sound("scp127_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhineSniff = sound("scp127_whine_sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> LevelUp = sound("scp127_level_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> LevelUpSurge = sound("scp127_level_up_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LevelUpHum = sound("scp127_level_up_hum");

    static {
        GunsRegistry.adoptGun(GunKind.Scp127, LivingGun);
    }

    private Scp127Registry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
