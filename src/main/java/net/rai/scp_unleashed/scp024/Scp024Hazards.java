package net.rai.scp_unleashed.scp024;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp024Hazards {
    private Scp024Hazards() {
    }

    public static DamageSource blastSource(ServerLevel World) {
        return World.damageSources().source(Scp024Registry.BlastDamage);
    }

    public static DamageSource eliminated(ServerLevel World) {
        return World.damageSources().source(Scp024Registry.EliminatedDamage);
    }

    public static void blast(Scp024Session Session, Vec3 Spot, double Radius, float Damage) {
        ServerLevel World = Session.World;
        World.sendParticles(ParticleTypes.EXPLOSION, Spot.x, Spot.y + 0.4, Spot.z, Radius > 2.5 ? 4 : 2, Radius * 0.25, 0.3, Radius * 0.25, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Spot.x, Spot.y + 0.5, Spot.z, 14, Radius * 0.3, 0.4, Radius * 0.3, 0.04);
        World.sendParticles(ParticleTypes.FLAME, Spot.x, Spot.y + 0.3, Spot.z, 10, Radius * 0.2, 0.2, Radius * 0.2, 0.08);
        Scp024Sounds.blast(World, Spot, (float) Math.min(1.4, 0.6 + Radius * 0.2));
        ServerPlayer User = Session.player();
        if (User == null) {
            return;
        }
        Vec3 Body = User.position().add(0.0, User.getBbHeight() * 0.5, 0.0);
        double Distance = Body.distanceTo(Spot);
        if (Distance >= Radius) {
            return;
        }
        double Strength = 1.0 - Distance / Radius;
        User.hurt(blastSource(World), (float) (Damage * Strength));
        Vec3 Away = new Vec3(Body.x - Spot.x, 0.0, Body.z - Spot.z);
        Away = Away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : Away.normalize();
        User.push(Away.x * 0.9 * Strength, 0.35 * Strength + 0.15, Away.z * 0.9 * Strength);
        User.hurtMarked = true;
    }

    public static void eliminate(ServerPlayer User) {
        User.hurt(eliminated(User.serverLevel()), Float.MAX_VALUE);
    }

    public static <T extends Mob> T contestantFoe(T Foe, double Health, double Damage, double Speed) {
        setBase(Foe, Attributes.MAX_HEALTH, Health);
        Foe.setHealth((float) Health);
        if (Damage >= 0.0) {
            setBase(Foe, Attributes.ATTACK_DAMAGE, Damage);
        }
        if (Speed >= 0.0) {
            setBase(Foe, Attributes.MOVEMENT_SPEED, Speed);
        }
        setBase(Foe, Attributes.FOLLOW_RANGE, 48.0);
        setBase(Foe, Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
        Foe.setCanPickUpLoot(false);
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            Foe.setDropChance(Slot, 0.0f);
        }
        Foe.setPersistenceRequired();
        return Foe;
    }

    public static void setBase(LivingEntity Target, Holder<Attribute> Kind, double Value) {
        AttributeInstance Instance = Target.getAttribute(Kind);
        if (Instance != null) {
            Instance.setBaseValue(Value);
        }
    }

    public static void hunt(Mob Foe, @Nullable ServerPlayer Target) {
        if (Target != null) {
            Foe.setTarget(Target);
        }
    }

    public static Arrow arrow(Scp024Session Session, Vec3 From, Vec3 To, float Speed, double Damage) {
        Arrow Shot = new Arrow(EntityType.ARROW, Session.World);
        Shot.setPos(From.x, From.y, From.z);
        Vec3 Path = To.subtract(From);
        Shot.shoot(Path.x, Path.y, Path.z, Speed, 0.6f);
        Shot.setBaseDamage(Damage);
        Shot.pickup = AbstractArrow.Pickup.DISALLOWED;
        Session.spawn(Shot);
        return Shot;
    }

    @Nullable
    public static Entity display(ServerLevel World, String Kind, Vec3 Spot, CompoundTag Extra) {
        CompoundTag Tag = Extra.copy();
        Tag.putString("id", Kind);
        ListTag Position = new ListTag();
        Position.add(DoubleTag.valueOf(Spot.x));
        Position.add(DoubleTag.valueOf(Spot.y));
        Position.add(DoubleTag.valueOf(Spot.z));
        Tag.put("Pos", Position);
        return EntityType.loadEntityRecursive(Tag, World, Created -> Created);
    }

    public static CompoundTag blockDisplay(BlockState State, float Scale, int Teleport) {
        CompoundTag Tag = new CompoundTag();
        Tag.put("block_state", NbtUtils.writeBlockState(State));
        Tag.putInt("teleport_duration", Teleport);
        Tag.put("transformation", transformation(-Scale * 0.5f, 0.0f, -Scale * 0.5f, Scale));
        CompoundTag Light = new CompoundTag();
        Light.putInt("block", 15);
        Light.putInt("sky", 15);
        Tag.put("brightness", Light);
        return Tag;
    }

    public static CompoundTag textDisplay(ServerLevel World, Component Text, float Scale, int Width, int Background, float Yaw) {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("text", Component.Serializer.toJson(Text, World.registryAccess()));
        Tag.putInt("line_width", Width);
        Tag.putInt("background", Background);
        Tag.putBoolean("shadow", true);
        Tag.putString("billboard", "vertical");
        Tag.put("transformation", transformation(0.0f, 0.0f, 0.0f, Scale));
        ListTag Rotation = new ListTag();
        Rotation.add(FloatTag.valueOf(Yaw));
        Rotation.add(FloatTag.valueOf(0.0f));
        Tag.put("Rotation", Rotation);
        CompoundTag Light = new CompoundTag();
        Light.putInt("block", 15);
        Light.putInt("sky", 15);
        Tag.put("brightness", Light);
        return Tag;
    }

    public static CompoundTag transformation(float X, float Y, float Z, float Scale) {
        CompoundTag Tag = new CompoundTag();
        Tag.put("left_rotation", floats(0.0f, 0.0f, 0.0f, 1.0f));
        Tag.put("right_rotation", floats(0.0f, 0.0f, 0.0f, 1.0f));
        Tag.put("translation", floats(X, Y, Z));
        Tag.put("scale", floats(Scale, Scale, Scale));
        return Tag;
    }

    private static ListTag floats(float... Values) {
        ListTag List = new ListTag();
        for (float Value : Values) {
            List.add(FloatTag.valueOf(Value));
        }
        return List;
    }

    public static ItemStack named(ItemStack Stack, String Key) {
        Stack.set(DataComponents.CUSTOM_NAME, Component.translatable(Key).withStyle(Style -> Style.withItalic(false)));
        return Stack;
    }

    public static ItemStack bow() {
        return named(new ItemStack(Items.BOW), "item.scp_unleashed.scp024_show_bow");
    }
}
