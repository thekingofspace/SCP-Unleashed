package net.rai.scp_unleashed.rocketry.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RocketDestination(ResourceLocation Id, ResourceKey<Level> Dimension, Component Name, ResourceLocation Icon, int Fuel, int Power, int ArrivalHeight,
                                float Gravity, int Order) {
    public static final int MaxFuel = 16000;
    public static final int MaxPower = 1000000;
    public static final Codec<Spec> SpecCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(Spec::Dimension),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Spec::Name),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Spec::Icon),
            Codec.intRange(1, MaxFuel).fieldOf("fuel").forGetter(Spec::Fuel),
            Codec.intRange(1, MaxPower).fieldOf("power").forGetter(Spec::Power),
            Codec.intRange(16, 512).optionalFieldOf("arrival_height", 160).forGetter(Spec::ArrivalHeight),
            Codec.floatRange(0.05f, 4.0f).optionalFieldOf("gravity", 1.0f).forGetter(Spec::Gravity),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Spec::Order)
    ).apply(Instance, Spec::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketDestination> DestinationStreamCodec = StreamCodec.of(
            (Buffer, Value) -> {
                ResourceLocation.STREAM_CODEC.encode(Buffer, Value.Id());
                ResourceLocation.STREAM_CODEC.encode(Buffer, Value.Dimension().location());
                ComponentSerialization.STREAM_CODEC.encode(Buffer, Value.Name());
                ResourceLocation.STREAM_CODEC.encode(Buffer, Value.Icon());
                ByteBufCodecs.VAR_INT.encode(Buffer, Value.Fuel());
                ByteBufCodecs.VAR_INT.encode(Buffer, Value.Power());
                ByteBufCodecs.VAR_INT.encode(Buffer, Value.ArrivalHeight());
                ByteBufCodecs.FLOAT.encode(Buffer, Value.Gravity());
                ByteBufCodecs.VAR_INT.encode(Buffer, Value.Order());
            },
            Buffer -> new RocketDestination(ResourceLocation.STREAM_CODEC.decode(Buffer),
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.STREAM_CODEC.decode(Buffer)),
                    ComponentSerialization.STREAM_CODEC.decode(Buffer), ResourceLocation.STREAM_CODEC.decode(Buffer),
                    ByteBufCodecs.VAR_INT.decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer),
                    ByteBufCodecs.FLOAT.decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer)));

    public record Spec(ResourceLocation Dimension, Component Name, ResourceLocation Icon, int Fuel, int Power, int ArrivalHeight, float Gravity, int Order) {
        public RocketDestination build(ResourceLocation Id) {
            return new RocketDestination(Id, ResourceKey.create(Registries.DIMENSION, Dimension), Name, Icon, Fuel, Power, ArrivalHeight, Gravity, Order);
        }
    }

    public boolean isEarth() {
        return Level.OVERWORLD.equals(Dimension);
    }
}
