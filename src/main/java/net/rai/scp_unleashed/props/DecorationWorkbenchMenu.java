package net.rai.scp_unleashed.props;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DecorationWorkbenchMenu extends AbstractContainerMenu {
    public static final int MaterialSlot = 0;
    public static final int ExtraSlot = 1;
    public static final int ResultSlot = 2;
    public static final int InventoryStart = 3;
    public static final int HotbarStart = 30;
    public static final int SlotsEnd = 39;
    public static final int MaterialX = 13;
    public static final int MaterialY = 17;
    public static final int ExtraX = 13;
    public static final int ExtraY = 45;
    public static final int ResultX = 13;
    public static final int ResultY = 90;
    public static final int InventoryX = 8;
    public static final int InventoryY = 128;
    public static final int HotbarY = 186;

    private final ContainerLevelAccess Access;
    private final Level World;
    private final List<RecipeHolder<DecorationRecipe>> Recipes;
    private final DataSlot Selected = DataSlot.standalone();
    private final ResultContainer Results = new ResultContainer();
    private final Slot MaterialInput;
    private final Slot ExtraInput;
    private final Slot Output;
    private long LastSound;
    private final Container Inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public DecorationWorkbenchMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, ContainerLevelAccess.NULL);
    }

    public DecorationWorkbenchMenu(int ContainerId, Inventory PlayerInventory, ContainerLevelAccess Access) {
        super(PropsRegistry.DecorationWorkbenchMenuType.get(), ContainerId);
        this.Access = Access;
        this.World = PlayerInventory.player.level();
        this.Recipes = DecorationRecipe.sorted(World.getRecipeManager());
        this.MaterialInput = addSlot(new Slot(Inputs, 0, MaterialX, MaterialY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return DecorationRecipe.usesMaterial(Recipes, Stack);
            }
        });
        this.ExtraInput = addSlot(new Slot(Inputs, 1, ExtraX, ExtraY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return DecorationRecipe.usesExtra(Recipes, Stack);
            }
        });
        this.Output = addSlot(new Slot(Results, 0, ResultX, ResultY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }

            @Override
            public void onTake(Player User, ItemStack Stack) {
                Stack.onCraftedBy(User.level(), User, Stack.getCount());
                Results.awardUsedRecipes(User, List.of(MaterialInput.getItem(), ExtraInput.getItem()));
                RecipeHolder<DecorationRecipe> Chosen = selectedRecipe();
                if (Chosen != null) {
                    MaterialInput.remove(Chosen.value().Material().count());
                    Chosen.value().Extra().ifPresent(Needed -> ExtraInput.remove(Needed.count()));
                }
                setupResult();
                Access.execute((Here, Pos) -> {
                    long Now = Here.getGameTime();
                    if (LastSound != Now) {
                        Here.playSound(null, Pos, PropsSounds.WorkbenchCraft.get(), SoundSource.BLOCKS, 1.0f, 0.95f + Here.random.nextFloat() * 0.1f);
                        LastSound = Now;
                    }
                });
                super.onTake(User, Stack);
            }
        });
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, InventoryY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, HotbarY));
        }
        Selected.set(-1);
        addDataSlot(Selected);
    }

    public List<RecipeHolder<DecorationRecipe>> getRecipes() {
        return Recipes;
    }

    public int getSelected() {
        return Selected.get();
    }

    public ItemStack getMaterial() {
        return MaterialInput.getItem();
    }

    public ItemStack getExtra() {
        return ExtraInput.getItem();
    }

    public boolean affordable(DecorationRecipe Plan) {
        return Plan.affordable(MaterialInput.getItem(), ExtraInput.getItem());
    }

    @Nullable
    public RecipeHolder<DecorationRecipe> selectedRecipe() {
        int Index = Selected.get();
        return Index >= 0 && Index < Recipes.size() ? Recipes.get(Index) : null;
    }

    private void setupResult() {
        RecipeHolder<DecorationRecipe> Chosen = selectedRecipe();
        DecorationInput Offered = new DecorationInput(MaterialInput.getItem(), ExtraInput.getItem());
        if (Chosen != null && Chosen.value().matches(Offered, World)) {
            Results.setRecipeUsed(Chosen);
            Output.set(Chosen.value().assemble(Offered, World.registryAccess()));
        } else {
            Output.set(ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Id < 0 || Id >= Recipes.size()) {
            return false;
        }
        Selected.set(Id);
        setupResult();
        return true;
    }

    @Override
    public void slotsChanged(Container Changed) {
        if (Changed == Inputs) {
            setupResult();
        }
    }

    @Override
    public boolean stillValid(Player User) {
        return stillValid(Access, User, PropsRegistry.DecorationWorkbench.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack Stack, Slot Target) {
        return Target.container != Results && super.canTakeItemForPickAll(Stack, Target);
    }

    private boolean moveIntoInputs(ItemStack Stack) {
        if (DecorationRecipe.usesMaterial(Recipes, Stack) && moveItemStackTo(Stack, MaterialSlot, MaterialSlot + 1, false)) {
            return true;
        }
        return DecorationRecipe.usesExtra(Recipes, Stack) && moveItemStackTo(Stack, ExtraSlot, ExtraSlot + 1, false);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Moved = Stack.copy();
        if (Index == ResultSlot) {
            Stack.getItem().onCraftedBy(Stack, User.level(), User);
            if (!moveItemStackTo(Stack, InventoryStart, SlotsEnd, true)) {
                return ItemStack.EMPTY;
            }
            Source.onQuickCraft(Stack, Moved);
        } else if (Index < InventoryStart) {
            if (!moveItemStackTo(Stack, InventoryStart, SlotsEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoInputs(Stack)) {
            if (Index < HotbarStart) {
                if (!moveItemStackTo(Stack, HotbarStart, SlotsEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(Stack, InventoryStart, HotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        }
        Source.setChanged();
        if (Stack.getCount() == Moved.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(User, Stack);
        broadcastChanges();
        return Moved;
    }

    @Override
    public void removed(Player User) {
        super.removed(User);
        Results.removeItemNoUpdate(0);
        Access.execute((Here, Pos) -> clearContainer(User, Inputs));
    }
}
