package net.rai.scp_unleashed.security;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum TurretStatus implements StringRepresentable {
    Offline("offline", ChatFormatting.GRAY, TurretLight.Off),
    Ready("ready", ChatFormatting.GREEN, TurretLight.Ready),
    Tracking("tracking", ChatFormatting.GOLD, TurretLight.Alert),
    Firing("firing", ChatFormatting.RED, TurretLight.Alert),
    Blocked("blocked", ChatFormatting.GOLD, TurretLight.Alert),
    NoAmmo("no_ammo", ChatFormatting.RED, TurretLight.Fault),
    Jammed("jammed", ChatFormatting.DARK_AQUA, TurretLight.Fault);

    private final String Name;
    private final ChatFormatting Colour;
    private final TurretLight Light;

    TurretStatus(String Name, ChatFormatting Colour, TurretLight Light) {
        this.Name = Name;
        this.Colour = Colour;
        this.Light = Light;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public TurretLight getLight() {
        return Light;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.sentry_turret.status." + Name;
    }

    public boolean isEngaged() {
        return this == Tracking || this == Firing || this == Blocked;
    }

    public static TurretStatus byIndex(int Index) {
        TurretStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Offline;
    }
}
