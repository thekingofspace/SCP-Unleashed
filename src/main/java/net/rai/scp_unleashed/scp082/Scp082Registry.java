package net.rai.scp_unleashed.scp082;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp082Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_082");
    public static final ResourceKey<DamageType> Devoured = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp082_devoured"));
    public static final TagKey<Block> RageProof = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp082_rage_proof"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp082Fernand>> Fernand = EntityTypeRegister.register("scp082_fernand",
            () -> EntityType.Builder.<Scp082Fernand>of(Scp082Fernand::new, MobCategory.MONSTER)
                    .sized(Scp082Fernand.Width, Scp082Fernand.Height)
                    .eyeHeight(Scp082Fernand.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp082_fernand"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp082_spawn_egg",
            () -> new DeferredSpawnEggItem(Fernand, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> Held = AttachmentRegister.register("scp082_held",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp082_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp082_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Voice = sound("scp082_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp082_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scp082_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grab = sound("scp082_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whiff = sound("scp082_whiff");
    public static final DeferredHolder<SoundEvent, SoundEvent> Struggle = sound("scp082_struggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chomp = sound("scp082_chomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crunch = sound("scp082_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chew = sound("scp082_chew");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burp = sound("scp082_burp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("scp082_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stomp = sound("scp082_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Smash = sound("scp082_smash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thud = sound("scp082_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = sound("scp082_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp082_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp082_death");

    private Scp082Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp082ShoreCondition.TypeName, Scp082ShoreCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp082Arrival::arrive);
    }
}
