package net.rai.scp_unleashed.dclass;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DClassInventoryMenu extends AbstractContainerMenu {
    public static final int ArmorX = 8;
    public static final int ArmorY = 8;
    public static final int OffhandX = 77;
    public static final int OffhandY = 62;
    public static final int GridX = 8;
    public static final int GridY = 93;
    public static final int HotbarY = 151;
    public static final int PlayerY = 182;
    public static final int PlayerHotbarY = 240;
    public static final int ArmorEnd = 4;
    public static final int OffhandEnd = 5;
    public static final int SubjectEnd = OffhandEnd + DClass.MainSize;
    public static final int PlayerEnd = SubjectEnd + 36;
    public static final int HealthData = 0;
    public static final int MaxHealthData = 1;
    public static final int FoodData = 2;
    public static final int SaturationData = 3;
    public static final int LevelData = 4;
    public static final int ArmorData = 5;
    public static final int DataCount = 6;
    private static final ResourceLocation[] EmptyArmor = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    @Nullable
    private final DClass Subject;
    private final Container Pack;
    private final ContainerData Data;

    public DClassInventoryMenu(int Id, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(Id, PlayerInventory, find(PlayerInventory.player, Buffer.readVarInt()));
    }

    public DClassInventoryMenu(int Id, Inventory PlayerInventory, @Nullable DClass Subject) {
        super(DClassRegistry.InventoryMenuType.get(), Id);
        this.Subject = Subject;
        this.Pack = Subject != null ? Subject.pack() : new SimpleContainer(DClassPack.Size);
        this.Data = Subject != null && !PlayerInventory.player.level().isClientSide ? liveData(Subject) : new SimpleContainerData(DataCount);
        for (int Index = 0; Index < 4; Index++) {
            EquipmentSlot Worn = DClassPack.ArmorOrder[Index];
            ResourceLocation Icon = EmptyArmor[Index];
            addSlot(new Slot(Pack, DClassPack.ArmorStart + Index, ArmorX, ArmorY + Index * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return Subject != null ? Subject.getEquipmentSlotForItem(Stack) == Worn : Stack.getEquipmentSlot() == Worn;
                }

                @Override
                public boolean mayPickup(Player User) {
                    ItemStack Held = getItem();
                    return Held.isEmpty() || User.isCreative() || !EnchantmentHelper.has(Held, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, Icon);
                }
            });
        }
        addSlot(new Slot(Pack, DClassPack.OffhandSlot, OffhandX, OffhandY) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(Pack, Column + Row * 9 + 9, GridX + Column * 18, GridY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(Pack, Column, GridX + Column * 18, HotbarY));
        }
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, GridX + Column * 18, PlayerY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, GridX + Column * 18, PlayerHotbarY));
        }
        addDataSlots(Data);
    }

    @Nullable
    private static DClass find(Player Viewer, int EntityId) {
        Entity Found = Viewer.level().getEntity(EntityId);
        return Found instanceof DClass Subject ? Subject : null;
    }

    private static ContainerData liveData(DClass Subject) {
        return new ContainerData() {
            @Override
            public int get(int Index) {
                return switch (Index) {
                    case HealthData -> Math.round(Subject.getHealth() * 10.0f);
                    case MaxHealthData -> Math.round(Subject.getMaxHealth() * 10.0f);
                    case FoodData -> Subject.food().food();
                    case SaturationData -> Math.round(Subject.food().saturation() * 10.0f);
                    case LevelData -> Subject.xpLevel();
                    case ArmorData -> Subject.getArmorValue();
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

    public int data(int Index) {
        return Data.get(Index);
    }

    public static void closeFor(DClass Subject) {
        if (!(Subject.level() instanceof ServerLevel World)) {
            return;
        }
        for (ServerPlayer Viewer : List.copyOf(World.getServer().getPlayerList().getPlayers())) {
            if (Viewer.containerMenu instanceof DClassInventoryMenu Menu && Menu.Subject == Subject) {
                Viewer.closeContainer();
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < SubjectEnd) {
            if (!moveItemStackTo(Stack, SubjectEnd, PlayerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean Moved = false;
            for (int Armour = 0; Armour < ArmorEnd && !Moved; Armour++) {
                Slot Target = slots.get(Armour);
                if (!Target.hasItem() && Target.mayPlace(Stack)) {
                    Moved = moveItemStackTo(Stack, Armour, Armour + 1, false);
                }
            }
            if (!Moved && !moveItemStackTo(Stack, OffhandEnd, SubjectEnd, false)) {
                return ItemStack.EMPTY;
            }
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
        return Subject != null && Subject.stillValidFor(User);
    }
}
