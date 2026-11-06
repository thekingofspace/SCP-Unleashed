package net.rai.scp_unleashed.scp107;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.FissionTags;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.Optional;

public enum Scp107RainKind {
    Water("water"),
    Lava("lava"),
    Caustic("waste"),
    Milk("milk"),
    Oil("oil");

    public static final TagKey<Fluid> CrudeOil = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "oil"));

    private final String Id;

    Scp107RainKind(String Id) {
        this.Id = Id;
    }

    public String id() {
        return Id;
    }

    public String key() {
        return "scp107.kind." + Id;
    }

    public boolean drawsSky() {
        return this != Water;
    }

    public boolean harmful() {
        return this == Lava || this == Caustic;
    }

    public Optional<Fluid> fluid() {
        return switch (this) {
            case Water -> Optional.of(Fluids.WATER);
            case Lava -> Optional.of(Fluids.LAVA);
            case Caustic -> Optional.of(FissionRegistry.WasteWater.get());
            case Milk -> Optional.empty();
            case Oil -> Optional.of(OilRegistry.Oil.getSource());
        };
    }

    public static Optional<Scp107RainKind> byOrdinal(int Index) {
        Scp107RainKind[] Kinds = values();
        return Index >= 0 && Index < Kinds.length ? Optional.of(Kinds[Index]) : Optional.empty();
    }

    public static Optional<Scp107RainKind> byId(String Wanted) {
        for (Scp107RainKind Kind : values()) {
            if (Kind.Id.equals(Wanted)) {
                return Optional.of(Kind);
            }
        }
        return Optional.empty();
    }

    public static Optional<Scp107RainKind> of(Fluid Liquid) {
        FluidState Sample = Liquid.defaultFluidState();
        if (Sample.isEmpty()) {
            return Optional.empty();
        }
        if (Sample.is(Tags.Fluids.MILK)) {
            return Optional.of(Milk);
        }
        if (Sample.is(CrudeOil)) {
            return Optional.of(Oil);
        }
        if (Sample.is(FissionTags.WasteWater)) {
            return Optional.of(Caustic);
        }
        if (Sample.is(FluidTags.LAVA)) {
            return Optional.of(Lava);
        }
        if (Sample.is(FluidTags.WATER)) {
            return Optional.of(Water);
        }
        return Optional.empty();
    }
}
