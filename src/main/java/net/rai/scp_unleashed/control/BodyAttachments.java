package net.rai.scp_unleashed.control;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class BodyAttachments {
    private static final Logger Log = LogUtils.getLogger();
    @Nullable
    private static Field SerializerField;
    @Nullable
    private static Field DeathField;
    private static boolean Resolved;

    private BodyAttachments() {
    }

    private static void resolve() {
        if (Resolved) {
            return;
        }
        Resolved = true;
        try {
            Field Serializer = AttachmentType.class.getDeclaredField("serializer");
            Field Death = AttachmentType.class.getDeclaredField("copyOnDeath");
            Serializer.setAccessible(true);
            Death.setAccessible(true);
            SerializerField = Serializer;
            DeathField = Death;
        } catch (Throwable Problem) {
            Log.warn("Control bodies cannot inspect attachment types; every serialised attachment will travel with the body", Problem);
        }
    }

    public static boolean serializable(AttachmentType<?> Type) {
        resolve();
        if (SerializerField == null) {
            return true;
        }
        try {
            return SerializerField.get(Type) != null;
        } catch (Throwable Problem) {
            return true;
        }
    }

    public static boolean accountBound(AttachmentType<?> Type) {
        resolve();
        if (DeathField == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(DeathField.get(Type));
        } catch (Throwable Problem) {
            return false;
        }
    }

    public static boolean isBody(AttachmentType<?> Type) {
        return Type != ControlRegistry.Session.get() && serializable(Type) && !accountBound(Type);
    }

    public static CompoundTag capture(Entity Holder) {
        CompoundTag Result = new CompoundTag();
        CompoundTag All = Holder.serializeAttachments(Holder.registryAccess());
        if (All == null) {
            return Result;
        }
        for (String Key : All.getAllKeys()) {
            ResourceLocation Id = ResourceLocation.tryParse(Key);
            AttachmentType<?> Type = Id == null ? null : NeoForgeRegistries.ATTACHMENT_TYPES.get(Id);
            if (Type != null && isBody(Type)) {
                Result.put(Key, All.get(Key).copy());
            }
        }
        return Result;
    }

    public static void clear(Entity Holder) {
        List<AttachmentType<?>> Present = new ArrayList<>();
        for (AttachmentType<?> Type : NeoForgeRegistries.ATTACHMENT_TYPES) {
            if (isBody(Type) && Holder.hasData(Type)) {
                Present.add(Type);
            }
        }
        for (AttachmentType<?> Type : Present) {
            Holder.removeData(Type);
        }
    }

    public static void apply(Entity Holder, CompoundTag Saved) {
        clear(Holder);
        if (Saved.isEmpty()) {
            return;
        }
        AttachmentHolder.AsField Carrier = new AttachmentHolder.AsField(Holder);
        Carrier.deserializeInternal(Holder.registryAccess(), Saved);
        for (String Key : Saved.getAllKeys()) {
            ResourceLocation Id = ResourceLocation.tryParse(Key);
            AttachmentType<?> Type = Id == null ? null : NeoForgeRegistries.ATTACHMENT_TYPES.get(Id);
            if (Type != null && isBody(Type)) {
                move(Type, Carrier, Holder);
            }
        }
    }

    private static <T> void move(AttachmentType<T> Type, IAttachmentHolder From, IAttachmentHolder To) {
        T Value = From.getExistingDataOrNull(Type);
        if (Value != null) {
            To.setData(Type, Value);
        }
    }
}
