package net.rai.scp_unleashed.dclass.duty;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum DClassTask {
    None,
    EatThis,
    FollowMe,
    CleanArea;

    public static final Codec<DClassTask> TaskCodec = Codec.STRING.xmap(DClassTask::byId, DClassTask::getId);

    public String getId() {
        return switch (this) {
            case None -> "none";
            case EatThis -> "eat_this";
            case FollowMe -> "follow_me";
            case CleanArea -> "clean_area";
        };
    }

    public Component getName() {
        return Component.translatable("dclass.scp_unleashed.task." + getId());
    }

    public boolean active() {
        return this != None;
    }

    public static DClassTask byIndex(int Index) {
        DClassTask[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : None;
    }

    public static DClassTask byId(String Id) {
        for (DClassTask Task : values()) {
            if (Task.getId().equals(Id) || Task.name().toLowerCase(Locale.ROOT).equals(Id)) {
                return Task;
            }
        }
        return None;
    }
}
