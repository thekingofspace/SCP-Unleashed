package net.rai.scp_unleashed.mining.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShopKind implements StringRepresentable {
    Item("item"),
    Entity("entity");

    public static final Codec<ShopKind> KindCodec = StringRepresentable.fromEnum(ShopKind::values);

    private final String SerializedName;

    ShopKind(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
