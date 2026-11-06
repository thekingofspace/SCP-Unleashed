package net.rai.scp_unleashed.props;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.decal.DecalKind;
import net.rai.scp_unleashed.decal.DecalRegistry;
import net.rai.scp_unleashed.decor.DecorRegistry;
import net.rai.scp_unleashed.decor.FacilityZone;
import net.rai.scp_unleashed.decor.LampColour;
import net.rai.scp_unleashed.decor.ZonePart;
import net.rai.scp_unleashed.utility.UtilityRegistry;

import java.util.function.Supplier;

public enum DecorationCategory implements StringRepresentable {
    Building("building", () -> DecorRegistry.zoneBlock(FacilityZone.Lcz, ZonePart.WallMiddle).get()),
    Lighting("lighting", () -> DecorRegistry.colouredLamp(LampColour.White).get()),
    Structures("structures", () -> UtilityRegistry.CatwalkItem.get()),
    Props("props", () -> PropsRegistry.OfficeChair.get()),
    Decals("decals", () -> DecalRegistry.Decals.get(DecalKind.HazardSign).get()),
    Misc("misc", () -> PropsRegistry.DecorationWorkbench.get());

    public static final Codec<DecorationCategory> CategoryCodec = StringRepresentable.fromEnum(DecorationCategory::values);
    public static final StreamCodec<ByteBuf, DecorationCategory> CategoryStreamCodec = ByteBufCodecs.idMapper(DecorationCategory::byId, DecorationCategory::ordinal);
    private static final DecorationCategory[] Ordered = values();

    private final String Name;
    private final Supplier<ItemLike> Icon;

    DecorationCategory(String Name, Supplier<ItemLike> Icon) {
        this.Name = Name;
        this.Icon = Icon;
    }

    public static DecorationCategory byId(int Id) {
        return Id >= 0 && Id < Ordered.length ? Ordered[Id] : Misc;
    }

    public ItemStack icon() {
        return new ItemStack(Icon.get());
    }

    public Component title() {
        return Component.translatable("gui.scp_unleashed.decoration_workbench.tab." + Name);
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
