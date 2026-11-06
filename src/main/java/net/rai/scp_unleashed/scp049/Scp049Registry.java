package net.rai.scp_unleashed.scp049;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp049Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_049");
    public static final ResourceKey<DamageType> CureDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_cure"));
    public static final ResourceKey<DamageType> FellDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_fell"));
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));
    public static final TagKey<Item> Repellent = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_repellent"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp049_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Robe = sound("scp049_robe");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheIn = sound("scp049_breathe_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheOut = sound("scp049_breathe_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheMask = sound("scp049_breathe_mask");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mutter = sound("scp049_mutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Greet = sound("scp049_greet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Touch = sound("scp049_touch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tools = sound("scp049_tools");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp049_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chant = sound("scp049_chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snip = sound("scp049_snip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Uncork = sound("scp049_uncork");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp049_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> Brew = sound("scp049_brew");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sweep = sound("scp049_sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tone = sound("scp049_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stinger = sound("scp049_stinger");
    public static final DeferredHolder<SoundEvent, SoundEvent> StingerLow = sound("scp049_stinger_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fell = sound("scp049_fell");
    public static final DeferredHolder<SoundEvent, SoundEvent> FellFade = sound("scp049_fell_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> GolemFall = sound("scp049_golem_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> GolemFallLow = sound("scp049_golem_fall_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drag = sound("scp049_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Command = sound("scp049_command");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp049_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp049_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Recoil = sound("scp049_recoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gasp = sound("scp049_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> CuredGroan = sound("scp049_2_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> CuredHurt = sound("scp049_2_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CuredDeath = sound("scp049_2_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CuredStep = sound("scp049_2_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CuredRise = sound("scp049_2_rise");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Miasma = ParticleRegister.register("scp049_miasma", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spore = ParticleRegister.register("scp049_spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp049_glint", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glow = ParticleRegister.register("scp049_glow", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CureSpore = ParticleRegister.register("scp049_cure_spore", () -> new SimpleParticleType(false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> Held = AttachmentRegister.register("scp049_held",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).build());

    public static final DeferredBlock<Scp049TrayBlock> Tray = BlockRegister.register("scp049_surgery_tray", () -> new Scp049TrayBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.BLOCK)
            .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> TrayItem = ItemRegister.register("scp049_surgery_tray",
            () -> new BlockItem(Tray.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp049TrayBlockEntity>> TrayEntity = BlockEntityTypeRegister.register("scp049_surgery_tray",
            () -> BlockEntityType.Builder.of(Scp049TrayBlockEntity::new, Tray.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp049Doctor>> Doctor = EntityTypeRegister.register("scp049",
            () -> EntityType.Builder.<Scp049Doctor>of(Scp049Doctor::new, MobCategory.MONSTER).sized(Scp049Doctor.Width, Scp049Doctor.Height)
                    .eyeHeight(Scp049Doctor.EyeHeight).clientTrackingRange(10).build("scp049"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp049Cured>> Cured = EntityTypeRegister.register("scp049_2",
            () -> EntityType.Builder.<Scp049Cured>of(Scp049Cured::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8)
                    .build("scp049_2"));

    public static final DeferredItem<DeferredSpawnEggItem> DoctorEgg = ItemRegister.register("scp049_spawn_egg",
            () -> new DeferredSpawnEggItem(Doctor, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CuredEgg = ItemRegister.register("scp049_2_spawn_egg",
            () -> new DeferredSpawnEggItem(Cured, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    private Scp049Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp049VillageCondition.TypeName, Scp049VillageCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp049Discovery::spawn);
    }
}
