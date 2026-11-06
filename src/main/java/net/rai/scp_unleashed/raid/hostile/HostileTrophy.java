package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record HostileTrophy(Kind Sort, ResourceLocation Type, CompoundTag Data, float Health, float MaxHealth) {
    public enum Kind {
        Creature("entity"),
        Thing("object");

        private final String Name;

        Kind(String Name) {
            this.Name = Name;
        }

        public String serialName() {
            return Name;
        }

        public static Kind byName(String Text) {
            for (Kind Each : values()) {
                if (Each.Name.equals(Text)) {
                    return Each;
                }
            }
            return Thing;
        }
    }

    public boolean creature() {
        return Sort == Kind.Creature;
    }

    @Nullable
    public static HostileTrophy of(Mob Target) {
        CompoundTag Saved = new CompoundTag();
        if (!Target.save(Saved)) {
            return null;
        }
        Saved.remove("Passengers");
        Saved.remove("leash");
        ResourceLocation Id = BuiltInRegistries.ENTITY_TYPE.getKey(Target.getType());
        return new HostileTrophy(Kind.Creature, Id, Saved, Target.getHealth(), Target.getMaxHealth());
    }

    @Nullable
    public static HostileTrophy of(ItemStack Stack, HolderLookup.Provider Lookup) {
        if (Stack.isEmpty()) {
            return null;
        }
        Tag Saved = Stack.save(Lookup);
        if (!(Saved instanceof CompoundTag Compound)) {
            return null;
        }
        return new HostileTrophy(Kind.Thing, BuiltInRegistries.ITEM.getKey(Stack.getItem()), Compound, 0.0f, 0.0f);
    }

    public ItemStack stack(HolderLookup.Provider Lookup) {
        if (creature()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(Lookup, Data).orElse(ItemStack.EMPTY);
    }

    public Optional<EntityType<?>> entityType() {
        return creature() ? EntityType.byString(Type.toString()) : Optional.empty();
    }

    public Component kindName() {
        if (creature()) {
            return entityType().<Component>map(EntityType::getDescription).orElseGet(() -> Component.literal(Type.toString()));
        }
        return BuiltInRegistries.ITEM.getOptional(Type).<Component>map(Found -> Found.getDescription()).orElseGet(() -> Component.literal(Type.toString()));
    }

    public Component label(HolderLookup.Provider Lookup) {
        if (creature()) {
            if (Data.contains("CustomName", Tag.TAG_STRING)) {
                try {
                    Component Named = Component.Serializer.fromJson(Data.getString("CustomName"), Lookup);
                    if (Named != null) {
                        return Named;
                    }
                } catch (RuntimeException Ignored) {
                    return kindName();
                }
            }
            return kindName();
        }
        ItemStack Found = stack(Lookup);
        return Found.isEmpty() ? kindName() : Found.getHoverName();
    }

    public HostileTrophy copy() {
        return new HostileTrophy(Sort, Type, Data.copy(), Health, MaxHealth);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putString("Kind", Sort.serialName());
        Saved.putString("Type", Type.toString());
        Saved.put("Data", Data.copy());
        Saved.putFloat("Health", Health);
        Saved.putFloat("MaxHealth", MaxHealth);
        return Saved;
    }

    @Nullable
    public static HostileTrophy load(CompoundTag Saved) {
        ResourceLocation Id = ResourceLocation.tryParse(Saved.getString("Type"));
        if (Id == null || !Saved.contains("Data", Tag.TAG_COMPOUND)) {
            return null;
        }
        return new HostileTrophy(Kind.byName(Saved.getString("Kind")), Id, Saved.getCompound("Data").copy(), Saved.getFloat("Health"), Saved.getFloat("MaxHealth"));
    }
}
