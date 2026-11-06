package net.rai.scp_unleashed.rocketry.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

import java.util.Optional;

public record LaunchRecord(Optional<GlobalPos> Site) {
    public static final LaunchRecord None = new LaunchRecord(Optional.empty());
    public static final Codec<LaunchRecord> RecordCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            GlobalPos.CODEC.optionalFieldOf("site").forGetter(LaunchRecord::Site)
    ).apply(Instance, LaunchRecord::new));
}
