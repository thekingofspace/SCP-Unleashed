package net.rai.scp_unleashed.stasis;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class StasisRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> SerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);

    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stasis_immune"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Charge = ComponentRegister.registerComponentType("stasis_pod_charge",
            Builder -> Builder.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StasisContents>> Contents = ComponentRegister.registerComponentType("stasis_pod_contents",
            Builder -> Builder.persistent(StasisContents.ContentsCodec).networkSynchronized(StasisContents.ContentsStreamCodec));

    public static final Map<FragmentRarity, DeferredItem<StasisPodItem>> Pods = registerPods();

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StasisUpgradeRecipe>> UpgradeSerializer = SerializerRegister.register("stasis_pod_upgrade",
            StasisUpgradeRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> Capture = sound("stasis_pod_capture");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaptureChime = sound("stasis_pod_capture_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaptureWarp = sound("stasis_pod_capture_warp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("stasis_pod_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReleaseHiss = sound("stasis_pod_release_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("stasis_pod_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleClink = sound("stasis_pod_rattle_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breakout = sound("stasis_pod_breakout");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakoutShards = sound("stasis_pod_breakout_shards");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakoutBurst = sound("stasis_pod_breakout_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> Deny = sound("stasis_pod_deny");

    private StasisRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        SerializerRegister.register(ModEventBus);
    }

    public static StasisPodItem podFor(FragmentRarity Grade) {
        return Pods.get(Grade).get();
    }

    private static Map<FragmentRarity, DeferredItem<StasisPodItem>> registerPods() {
        Map<FragmentRarity, DeferredItem<StasisPodItem>> Result = new EnumMap<>(FragmentRarity.class);
        for (FragmentRarity Grade : FragmentRarity.values()) {
            Result.put(Grade, ItemRegister.register("stasis_pod_" + Grade.getSerializedName(),
                    () -> new StasisPodItem(Grade, new Item.Properties().stacksTo(1))));
        }
        return Collections.unmodifiableMap(Result);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
