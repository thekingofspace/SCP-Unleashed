package net.rai.scp_unleashed.stasis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StasisContents(CompoundTag EntityData, Component Label, Component Kind, float Health, float MaxHealth, long Expiry) {
    public static final Codec<StasisContents> ContentsCodec = RecordCodecBuilder.create(Fields -> Fields.group(
            CompoundTag.CODEC.fieldOf("entity").forGetter(StasisContents::EntityData),
            ComponentSerialization.CODEC.fieldOf("label").forGetter(StasisContents::Label),
            ComponentSerialization.CODEC.fieldOf("kind").forGetter(StasisContents::Kind),
            Codec.FLOAT.optionalFieldOf("health", 0.0f).forGetter(StasisContents::Health),
            Codec.FLOAT.optionalFieldOf("max_health", 0.0f).forGetter(StasisContents::MaxHealth),
            Codec.LONG.fieldOf("expiry").forGetter(StasisContents::Expiry)
    ).apply(Fields, StasisContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StasisContents> ContentsStreamCodec = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, StasisContents::EntityData,
            ComponentSerialization.STREAM_CODEC, StasisContents::Label,
            ComponentSerialization.STREAM_CODEC, StasisContents::Kind,
            ByteBufCodecs.FLOAT, StasisContents::Health,
            ByteBufCodecs.FLOAT, StasisContents::MaxHealth,
            ByteBufCodecs.VAR_LONG, StasisContents::Expiry,
            StasisContents::new);

    public CompoundTag copyData() {
        return EntityData.copy();
    }

    public boolean isNamed() {
        return EntityData.contains("CustomName");
    }

    public long remaining(long Now) {
        return Math.max(0L, Expiry - Now);
    }

    public boolean expired(long Now) {
        return Now >= Expiry;
    }
}
