package net.rai.scp_unleashed.dclass.duty;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum DClassRole {
    None(false),
    Janitor(true);

    public static final Codec<DClassRole> RoleCodec = Codec.STRING.xmap(DClassRole::byId, DClassRole::getId);

    private final boolean Homed;

    DClassRole(boolean Homed) {
        this.Homed = Homed;
    }

    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component getName() {
        return Component.translatable("dclass.scp_unleashed.role." + getId());
    }

    public boolean homed() {
        return Homed;
    }

    public boolean active() {
        return this != None;
    }

    public static DClassRole byIndex(int Index) {
        DClassRole[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : None;
    }

    public static DClassRole byId(String Id) {
        for (DClassRole Role : values()) {
            if (Role.getId().equals(Id)) {
                return Role;
            }
        }
        return None;
    }
}
