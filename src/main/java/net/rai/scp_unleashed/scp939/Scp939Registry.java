package net.rai.scp_unleashed.scp939;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.scp939.compressor.AmnesticCanisterItem;
import net.rai.scp_unleashed.scp939.compressor.AmnesticEffect;
import net.rai.scp_unleashed.scp939.compressor.EmptyCanisterItem;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlock;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorMenu;

import java.util.List;

public final class Scp939Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_939");
    public static final String CompressorId = "gas_compressor";
    public static final String EmptyCanisterId = "empty_canister";
    public static final String CanisterId = "amnestic_canister";
    public static final TagKey<MobEffect> Amnestics = TagKey.create(Registries.MOB_EFFECT, location("amnestic"));
    public static final float CompressorHardness = 3.5f;
    public static final float CompressorResistance = 6.0f;
    public static final int CompressorLight = 3;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp939>> Scp939Type = EntityTypeRegister.register("scp939",
            () -> EntityType.Builder.<Scp939>of(Scp939::new, MobCategory.MONSTER)
                    .sized(Scp939.Width, Scp939.Height)
                    .eyeHeight(Scp939.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp939"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp939_spawn_egg",
            () -> new DeferredSpawnEggItem(Scp939Type, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredBlock<GasCompressorBlock> Compressor = BlockRegister.register(CompressorId,
            () -> new GasCompressorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .strength(CompressorHardness, CompressorResistance).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(State -> State.getValue(GasCompressorBlock.Active) ? CompressorLight : 0)));
    public static final DeferredItem<BlockItem> CompressorItem = ItemRegister.register(CompressorId, () -> new BlockItem(Compressor.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCompressorBlockEntity>> CompressorEntity = BlockEntityTypeRegister.register(CompressorId,
            () -> BlockEntityType.Builder.of(GasCompressorBlockEntity::new, Compressor.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GasCompressorMenu>> CompressorMenu = MenuTypeRegister.register(CompressorId,
            () -> IMenuTypeExtension.create(GasCompressorMenu::new));

    public static final DeferredItem<EmptyCanisterItem> EmptyCanister = ItemRegister.register(EmptyCanisterId, () -> new EmptyCanisterItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AmnesticCanisterItem> Canister = ItemRegister.register(CanisterId,
            () -> new AmnesticCanisterItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<MobEffect, AmnesticEffect> Amnestic = MobEffectRegister.register("amnestic", AmnesticEffect::new);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> Exposure = AttachmentRegister.register("scp939_exposure",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT, Saved -> Saved > 0.0f).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> Strain = AttachmentRegister.register("amnestic_strain",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT, Saved -> Saved > 0.0f).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Calm = AttachmentRegister.register("scp939_calm",
            () -> AttachmentType.builder(() -> 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> Sent = AttachmentRegister.register("scp939_sent",
            () -> AttachmentType.builder(() -> -1L).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp939Brood.State>> Brood = AttachmentRegister.register("scp939_brood",
            () -> AttachmentType.builder(() -> Scp939Brood.State.None).serialize(Scp939Brood.State.StateCodec, Scp939Brood.State::active).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scp939_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowlRumble = sound("scp939_growl_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breathe = sound("scp939_breathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheWet = sound("scp939_breathe_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Teeth = sound("scp939_teeth");
    public static final DeferredHolder<SoundEvent, SoundEvent> TeethClick = sound("scp939_teeth_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp939_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepPad = sound("scp939_step_pad");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepHeavy = sound("scp939_step_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> LureHmm = sound("scp939_lure_hmm");
    public static final DeferredHolder<SoundEvent, SoundEvent> LureTrade = sound("scp939_lure_trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> LureYes = sound("scp939_lure_yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> LureNo = sound("scp939_lure_no");
    public static final DeferredHolder<SoundEvent, SoundEvent> LureHurt = sound("scp939_lure_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp939_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> BiteSnap = sound("scp939_bite_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = sound("scp939_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LungeRoar = sound("scp939_lunge_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp939_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatCrunch = sound("scp939_eat_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sniff = sound("scp939_sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SniffHuff = sound("scp939_sniff_huff");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp939_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtHiss = sound("scp939_hurt_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp939_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathRattle = sound("scp939_death_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heave = sound("scp939_heave");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeaveWet = sound("scp939_heave_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shiver = sound("scp939_shiver");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShiverGroan = sound("scp939_shiver_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp939_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstMist = sound("scp939_burst_mist");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicCow = sound("scp939_mimic_cow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicSheep = sound("scp939_mimic_sheep");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicPig = sound("scp939_mimic_pig");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicChicken = sound("scp939_mimic_chicken");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicVillager = sound("scp939_mimic_villager");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicPerson = sound("scp939_mimic_person");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicGoat = sound("scp939_mimic_goat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Inhale = sound("amnestic_canister_inhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> InhaleHiss = sound("amnestic_canister_inhale_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Muffled = sound("amnestic_canister_muffled");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("gas_compressor_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HissAir = sound("gas_compressor_hiss_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thump = sound("gas_compressor_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThumpClank = sound("gas_compressor_thump_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound("gas_compressor_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound("gas_compressor_stop");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowlLayers = List.of(Growl, GrowlRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreatheLayers = List.of(Breathe, BreatheWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TeethLayers = List.of(Teeth, TeethClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepPad);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(Bite, BiteSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(Lunge, LungeRoar);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SniffLayers = List.of(Sniff, SniffHuff);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathRattle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HeaveLayers = List.of(Heave, HeaveWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShiverLayers = List.of(Shiver, ShiverGroan);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstMist);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> InhaleLayers = List.of(Inhale, InhaleHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HissLayers = List.of(Hiss, HissAir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThumpLayers = List.of(Thump, ThumpClank);

    private Scp939Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp939CaveCondition.TypeName, Scp939CaveCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp939Cave::manifest);
    }
}
