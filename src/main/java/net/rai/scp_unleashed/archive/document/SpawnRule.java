package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum SpawnRule implements StringRepresentable {
    Once("once"),
    Repeatable("repeatable");

    public static final Codec<SpawnRule> RuleCodec = StringRepresentable.fromEnum(SpawnRule::values);

    private final String SerializedName;

    SpawnRule(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public Component getName() {
        return Component.translatable("archive.scp_unleashed.spawn." + SerializedName);
    }
}
