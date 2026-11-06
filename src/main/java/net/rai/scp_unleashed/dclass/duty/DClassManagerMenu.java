package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.guards.GuardDuty;
import org.jetbrains.annotations.Nullable;

public class DClassManagerMenu extends AbstractContainerMenu {
    public static final int MealX = 12;
    public static final int MealY = 80;
    public static final int PlayerX = 8;
    public static final int PlayerY = 130;
    public static final int HotbarY = 188;
    public static final int RoleButton = 0;
    public static final int EatButton = 20;
    public static final int FollowButton = 21;
    public static final int CleanButton = 22;
    public static final int CancelButton = 23;
    public static final int RoleData = 0;
    public static final int TaskData = 1;
    public static final int PatrolData = 2;
    public static final int DataCount = 3;
    public static final int MealSlot = 0;
    public static final int PlayerStart = 1;
    public static final int PlayerEnd = PlayerStart + 36;

    @Nullable
    private final DClass Subject;
    private final SimpleContainer Meal = new SimpleContainer(1);
    private final ContainerData Data;

    public DClassManagerMenu(int Id, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(Id, PlayerInventory, find(PlayerInventory.player, Buffer.readVarInt()));
    }

    public DClassManagerMenu(int Id, Inventory PlayerInventory, @Nullable DClass Subject) {
        super(DClassDutyRegistry.ManagerMenuType.get(), Id);
        this.Subject = Subject;
        this.Data = Subject != null && !PlayerInventory.player.level().isClientSide ? liveData(Subject) : new SimpleContainerData(DataCount);
        addSlot(new Slot(Meal, 0, MealX, MealY) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, PlayerX + Column * 18, PlayerY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, PlayerX + Column * 18, HotbarY));
        }
        addDataSlots(Data);
    }

    @Nullable
    private static DClass find(Player Viewer, int EntityId) {
        Entity Found = Viewer.level().getEntity(EntityId);
        return Found instanceof DClass Body ? Body : null;
    }

    private static ContainerData liveData(DClass Subject) {
        return new ContainerData() {
            @Override
            public int get(int Index) {
                DClassDuty Duty = DClassDuties.of(Subject);
                return switch (Index) {
                    case RoleData -> Duty.Role().ordinal();
                    case TaskData -> Duty.Task().ordinal();
                    case PatrolData -> GuardDuty.has(Subject) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int Index, int Value) {
            }

            @Override
            public int getCount() {
                return DataCount;
            }
        };
    }

    @Nullable
    public DClass subject() {
        return Subject;
    }

    public DClassRole role() {
        return DClassRole.byIndex(Data.get(RoleData));
    }

    public DClassTask task() {
        return DClassTask.byIndex(Data.get(TaskData));
    }

    public boolean patrolling() {
        return Data.get(PatrolData) != 0;
    }

    public ItemStack meal() {
        return Meal.getItem(0);
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Subject == null || !(User instanceof ServerPlayer Server) || !DClassDuties.available(Subject)) {
            return false;
        }
        boolean Done;
        int Roles = DClassRole.values().length;
        if (Id >= RoleButton && Id < RoleButton + Roles) {
            DClassRole Role = DClassRole.byIndex(Id - RoleButton);
            Done = DClassDuties.assignRole(Subject, Role);
            if (Done) {
                DClassDuties.tell(Server, Role.active() ? "role" : "role_cleared", Subject.getName(), Role.getName());
            }
        } else {
            Done = switch (Id) {
                case EatButton -> eat(Server);
                case FollowButton -> follow(Server);
                case CleanButton -> clean(Server);
                case CancelButton -> cancel(Server);
                default -> false;
            };
        }
        if (Done) {
            scribble(Server);
            broadcastChanges();
        }
        return Done;
    }

    private boolean eat(ServerPlayer User) {
        if (Subject == null) {
            return false;
        }
        ItemStack Offered = Meal.getItem(0);
        DClassDuties.Verdict Result = DClassDuties.orderEat(Subject, User, Offered);
        switch (Result) {
            case Accepted -> {
                DClassDuties.tell(User, "eat", Subject.getName(), Offered.getHoverName());
                Meal.removeItem(0, 1);
                return true;
            }
            case Empty -> DClassDuties.warn(User, "empty");
            case Unusable -> DClassDuties.warn(User, "unusable", Subject.getName());
            case Full -> DClassDuties.warn(User, "full", Subject.getName());
            case NotHungry -> DClassDuties.warn(User, "not_hungry", Subject.getName());
            case Unavailable -> DClassDuties.warn(User, "unavailable");
        }
        if (!Offered.isEmpty() && Result != DClassDuties.Verdict.Empty) {
            User.getInventory().placeItemBackInInventory(Meal.removeItemNoUpdate(0));
        }
        return false;
    }

    private boolean follow(ServerPlayer User) {
        if (Subject == null || !DClassDuties.orderFollow(Subject, User)) {
            return false;
        }
        DClassDuties.tell(User, "follow", Subject.getName());
        return true;
    }

    private boolean clean(ServerPlayer User) {
        if (Subject == null || !DClassDuties.orderClean(Subject, User)) {
            return false;
        }
        DClassDuties.tell(User, "clean", Subject.getName());
        return true;
    }

    private boolean cancel(ServerPlayer User) {
        if (Subject == null) {
            return false;
        }
        boolean Cleared = DClassDuties.clearTask(Subject) || GuardDuty.clear(Subject);
        DClassDuties.tell(User, Cleared ? "cancel" : "nothing", Subject.getName());
        return Cleared;
    }

    private void scribble(ServerPlayer User) {
        float Pitch = 0.95f + User.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : DClassDutyRegistry.ManagerLayers) {
            User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, 0.7f, Pitch);
        }
    }

    @Override
    public void removed(Player User) {
        super.removed(User);
        clearContainer(User, Meal);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index == MealSlot) {
            if (!moveItemStackTo(Stack, PlayerStart, PlayerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            Slot Target = slots.get(MealSlot);
            if (Target.hasItem()) {
                return ItemStack.EMPTY;
            }
            Target.setByPlayer(Stack.split(1));
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        } else {
            Source.setChanged();
        }
        if (Stack.getCount() == Original.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(User, Stack);
        return Original;
    }

    @Override
    public boolean stillValid(Player User) {
        return Subject != null && Subject.stillValidFor(User) && DClassDuties.available(Subject);
    }
}
