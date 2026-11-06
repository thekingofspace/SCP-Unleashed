package net.rai.scp_unleashed.faction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Factions {
    private static final Map<ResourceLocation, FactionType> Types = new LinkedHashMap<>();

    private Factions() {
    }

    public static synchronized <T extends FactionType> T register(T Type) {
        if (Types.containsKey(Type.id())) {
            throw new IllegalStateException("faction " + Type.id() + " registered twice");
        }
        Types.put(Type.id(), Type);
        return Type;
    }

    public static Collection<FactionType> all() {
        return Collections.unmodifiableCollection(Types.values());
    }

    @Nullable
    public static FactionType get(ResourceLocation Id) {
        FactionType Found = Types.get(Id);
        if (Found == null && "minecraft".equals(Id.getNamespace())) {
            for (FactionType Type : Types.values()) {
                if (Type.id().getPath().equals(Id.getPath())) {
                    return Type;
                }
            }
        }
        return Found;
    }

    public static FactionBond bondOf(Entity Body) {
        return Body.hasData(FactionRegistry.Bond) ? Body.getData(FactionRegistry.Bond) : FactionBond.None;
    }

    @Nullable
    public static FactionType factionOf(Entity Body) {
        FactionBond Bond = bondOf(Body);
        if (!Bond.bound()) {
            return null;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Bond.Faction());
        return Id == null ? null : Types.get(Id);
    }
}
