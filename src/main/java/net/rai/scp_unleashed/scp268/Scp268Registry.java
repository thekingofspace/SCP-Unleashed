package net.rai.scp_unleashed.scp268;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.List;

public final class Scp268Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final String CapId = "scp268_cap";
    public static final ResourceLocation DocumentId = location("scp_268");

    public static final DeferredHolder<SoundEvent, SoundEvent> Don = sound("scp268_don");
    public static final DeferredHolder<SoundEvent, SoundEvent> DonBrush = sound("scp268_don_brush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Doff = sound("scp268_doff");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoffBrush = sound("scp268_doff_brush");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DonLayers = List.of(Don, DonBrush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DoffLayers = List.of(Doff, DoffBrush);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Tweed = MaterialRegister.register("scp268_tweed", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type Kind : ArmorItem.Type.values()) {
            Defense.put(Kind, 0);
        }
        return new ArmorMaterial(Defense, 0, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(location(CapId))), 0.0f, 0.0f);
    });

    public static final DeferredItem<Scp268CapItem> Cap = ItemRegister.register(CapId,
            () -> new Scp268CapItem(Tweed, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private Scp268Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
    }
}
