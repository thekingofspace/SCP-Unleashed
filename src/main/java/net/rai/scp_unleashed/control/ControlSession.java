package net.rai.scp_unleashed.control;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public record ControlSession(CompoundTag Home, ResourceLocation HomeDimension, double HomeX, double HomeY, double HomeZ, float HomeYaw, float HomePitch,
                             Optional<UUID> Vacant, ResourceLocation BodyType, UUID BodyId, CompoundTag Identity, long Started) {
    public static final ResourceLocation NoBody = ResourceLocation.withDefaultNamespace("empty");
    public static final ControlSession None = new ControlSession(new CompoundTag(), Level.OVERWORLD.location(), 0.0, 0.0, 0.0, 0.0f, 0.0f,
            Optional.empty(), NoBody, Util.NIL_UUID, new CompoundTag(), 0L);
    public static final Codec<ControlSession> SessionCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            CompoundTag.CODEC.fieldOf("home").forGetter(ControlSession::Home),
            ResourceLocation.CODEC.fieldOf("home_dimension").forGetter(ControlSession::HomeDimension),
            Codec.DOUBLE.fieldOf("home_x").forGetter(ControlSession::HomeX),
            Codec.DOUBLE.fieldOf("home_y").forGetter(ControlSession::HomeY),
            Codec.DOUBLE.fieldOf("home_z").forGetter(ControlSession::HomeZ),
            Codec.FLOAT.fieldOf("home_yaw").forGetter(ControlSession::HomeYaw),
            Codec.FLOAT.fieldOf("home_pitch").forGetter(ControlSession::HomePitch),
            UUIDUtil.CODEC.optionalFieldOf("vacant").forGetter(ControlSession::Vacant),
            ResourceLocation.CODEC.fieldOf("body_type").forGetter(ControlSession::BodyType),
            UUIDUtil.CODEC.fieldOf("body_id").forGetter(ControlSession::BodyId),
            CompoundTag.CODEC.fieldOf("identity").forGetter(ControlSession::Identity),
            Codec.LONG.fieldOf("started").forGetter(ControlSession::Started)
    ).apply(Instance, ControlSession::new));

    public boolean active() {
        return !Home.isEmpty() && !Util.NIL_UUID.equals(BodyId);
    }

    public ResourceKey<Level> homeLevel() {
        return ResourceKey.create(Registries.DIMENSION, HomeDimension);
    }

    public ControlSession withBody(ResourceLocation Type, UUID Id, CompoundTag NewIdentity) {
        return new ControlSession(Home, HomeDimension, HomeX, HomeY, HomeZ, HomeYaw, HomePitch, Vacant, Type, Id, NewIdentity, Started);
    }
}
