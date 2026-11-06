package net.rai.scp_unleashed.scp465;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp465Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_465");
    public static final ResourceKey<DamageType> ConfettiDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp465_confetti"));
    public static final int ChocolateNutrition = 3;
    public static final float ChocolateSaturation = 0.4f;
    public static final int PunchNutrition = 2;
    public static final float PunchSaturation = 0.4f;
    public static final int CupcakeNutrition = 4;
    public static final float CupcakeSaturation = 0.5f;
    public static final int ChipsNutrition = 2;
    public static final float ChipsSaturation = 0.3f;
    public static final int FoodStack = 16;

    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp465_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpenPop = sound("scp465_lid_open_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClose = sound("scp465_lid_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidCloseThump = sound("scp465_lid_close_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp465_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleKnock = sound("scp465_rattle_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp465_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstTwinkle = sound("scp465_burst_twinkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cheer = sound("scp465_cheer");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerCrowd = sound("scp465_cheer_crowd");
    public static final DeferredHolder<SoundEvent, SoundEvent> Honk = sound("scp465_honk");
    public static final DeferredHolder<SoundEvent, SoundEvent> HonkBuzz = sound("scp465_honk_buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp465_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Disintegrate = sound("scp465_disintegrate");
    public static final DeferredHolder<SoundEvent, SoundEvent> DisintegratePop = sound("scp465_disintegrate_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DisintegrateShimmer = sound("scp465_disintegrate_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = sound("scp465_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> VanishShimmer = sound("scp465_vanish_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Popper = sound("scp465_popper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clap = sound("scp465_clap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Serve = sound("scp465_serve");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp465_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> LetterOpen = sound("scp465_letter_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gulp = sound("scp465_gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crunch = sound("scp465_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxStep = sound("scp465_box_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxPlace = sound("scp465_box_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxBreak = sound("scp465_box_break");

    public static final DeferredHolder<SoundEvent, SoundEvent> MusicKick = sound("scp465_music_kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicKickSub = sound("scp465_music_kick_sub");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicClap = sound("scp465_music_clap");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicClapBody = sound("scp465_music_clap_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicHat = sound("scp465_music_hat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicHatOpen = sound("scp465_music_hat_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBass = sound("scp465_music_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBassAttack = sound("scp465_music_bass_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicArp = sound("scp465_music_arp");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicLead = sound("scp465_music_lead");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicLeadShine = sound("scp465_music_lead_shine");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicStab = sound("scp465_music_stab");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicCrash = sound("scp465_music_crash");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(LidOpen, LidOpenPop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(LidClose, LidCloseThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleKnock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstTwinkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CheerLayers = List.of(Cheer, CheerCrowd);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HonkLayers = List.of(Honk, HonkBuzz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DisintegrateLayers = List.of(Disintegrate, DisintegratePop, DisintegrateShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VanishLayers = List.of(Vanish, VanishShimmer);

    public static final DeferredSoundType Cardboard = new DeferredSoundType(0.8f, 1.1f, BoxBreak, BoxStep, BoxPlace, BoxStep, BoxStep);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Confetti = ParticleRegister.register("scp465_confetti", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Streamer = ParticleRegister.register("scp465_streamer", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp465BoxBlock> Box = BlockRegister.register("scp465_box",
            () -> new Scp465BoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(0.6f, 1200.0f)
                    .sound(Cardboard).noOcclusion().forceSolidOn().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp465BoxItem> BoxItem = ItemRegister.register("scp465_box",
            () -> new Scp465BoxItem(Box.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp465BoxBlockEntity>> BoxEntity = BlockEntityTypeRegister.register("scp465_box",
            () -> BlockEntityType.Builder.of(Scp465BoxBlockEntity::new, Box.get()).build(null));

    private static final Map<Scp465Decor, DeferredBlock<Scp465PartyBlock>> Decor = new EnumMap<>(Scp465Decor.class);
    private static final Map<Scp465Decor, DeferredItem<BlockItem>> DecorItems = new EnumMap<>(Scp465Decor.class);
    private static final List<DeferredItem<BlockItem>> DecorOrder = new ArrayList<>();

    static {
        for (Scp465Decor Kind : Scp465Decor.values()) {
            DeferredBlock<Scp465PartyBlock> Made = BlockRegister.register(Kind.id(), () -> new Scp465PartyBlock(Kind, Kind.properties()));
            Decor.put(Kind, Made);
            DeferredItem<BlockItem> Carried = ItemRegister.register(Kind.id(), () -> new Scp465PartyBlockItem(Made.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
            DecorItems.put(Kind, Carried);
            DecorOrder.add(Carried);
        }
    }

    public static final DeferredItem<Scp465LetterItem> Letter = ItemRegister.register("scp465_letter",
            () -> new Scp465LetterItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp465FoodItem> Chocolate = ItemRegister.register("scp465_chocolate",
            () -> new Scp465FoodItem(Scp465FoodItem.Kind.Chocolate, new Item.Properties().stacksTo(FoodStack)
                    .food(new FoodProperties.Builder().nutrition(ChocolateNutrition).saturationModifier(ChocolateSaturation).fast().build())));
    public static final DeferredItem<Scp465FoodItem> Punch = ItemRegister.register("scp465_punch",
            () -> new Scp465FoodItem(Scp465FoodItem.Kind.Punch, new Item.Properties().stacksTo(FoodStack)
                    .food(new FoodProperties.Builder().nutrition(PunchNutrition).saturationModifier(PunchSaturation).alwaysEdible().build())));
    public static final DeferredItem<Scp465FoodItem> Cupcake = ItemRegister.register("scp465_cupcake",
            () -> new Scp465FoodItem(Scp465FoodItem.Kind.Cupcake, new Item.Properties().stacksTo(FoodStack)
                    .food(new FoodProperties.Builder().nutrition(CupcakeNutrition).saturationModifier(CupcakeSaturation).build())));
    public static final DeferredItem<Scp465FoodItem> Chips = ItemRegister.register("scp465_chips",
            () -> new Scp465FoodItem(Scp465FoodItem.Kind.Chips, new Item.Properties().stacksTo(FoodStack)
                    .food(new FoodProperties.Builder().nutrition(ChipsNutrition).saturationModifier(ChipsSaturation).fast().build())));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp465Guest>> Guest = EntityTypeRegister.register("scp465_instance",
            () -> EntityType.Builder.<Scp465Guest>of(Scp465Guest::new, MobCategory.MISC)
                    .sized(Scp465Guest.Width, Scp465Guest.Height)
                    .eyeHeight(Scp465Guest.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp465_instance"));

    private Scp465Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static Scp465PartyBlock decor(Scp465Decor Kind) {
        return Decor.get(Kind).get();
    }

    public static BlockItem decorItem(Scp465Decor Kind) {
        return DecorItems.get(Kind).get();
    }

    public static List<DeferredItem<BlockItem>> decorItems() {
        return Collections.unmodifiableList(DecorOrder);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
