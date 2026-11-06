package net.rai.scp_unleashed.dclass.duty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record DClassDuty(DClassRole Role, Optional<BlockPos> Home, DClassTask Task, Optional<BlockPos> Spot, Optional<UUID> Issuer, ItemStack Meal, long Since) {
    public static final DClassDuty Idle = new DClassDuty(DClassRole.None, Optional.empty(), DClassTask.None, Optional.empty(), Optional.empty(), ItemStack.EMPTY, 0L);

    public static final Codec<DClassDuty> DutyCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            DClassRole.RoleCodec.optionalFieldOf("Role", DClassRole.None).forGetter(DClassDuty::Role),
            BlockPos.CODEC.optionalFieldOf("Home").forGetter(DClassDuty::Home),
            DClassTask.TaskCodec.optionalFieldOf("Task", DClassTask.None).forGetter(DClassDuty::Task),
            BlockPos.CODEC.optionalFieldOf("Spot").forGetter(DClassDuty::Spot),
            UUIDUtil.CODEC.optionalFieldOf("Issuer").forGetter(DClassDuty::Issuer),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("Meal", ItemStack.EMPTY).forGetter(DClassDuty::Meal),
            Codec.LONG.optionalFieldOf("Since", 0L).forGetter(DClassDuty::Since)
    ).apply(Instance, DClassDuty::new));

    public boolean idle() {
        return !Role.active() && !Task.active();
    }

    public boolean busy() {
        return !idle();
    }

    public DClassDuty withRole(DClassRole NewRole, @Nullable BlockPos NewHome) {
        return new DClassDuty(NewRole, NewRole.active() ? Optional.ofNullable(NewHome) : Optional.empty(), Task, Spot, Issuer, Meal, Since);
    }

    public DClassDuty withTask(DClassTask NewTask, @Nullable BlockPos NewSpot, @Nullable UUID NewIssuer, ItemStack NewMeal, long Now) {
        if (!NewTask.active()) {
            return withoutTask();
        }
        return new DClassDuty(Role, Home, NewTask, Optional.ofNullable(NewSpot), Optional.ofNullable(NewIssuer), NewMeal.copy(), Now);
    }

    public DClassDuty withoutTask() {
        return new DClassDuty(Role, Home, DClassTask.None, Optional.empty(), Optional.empty(), ItemStack.EMPTY, 0L);
    }

    public DClassDuty withoutRole() {
        return new DClassDuty(DClassRole.None, Optional.empty(), Task, Spot, Issuer, Meal, Since);
    }
}
