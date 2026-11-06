package net.rai.scp_unleashed.stasis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.sealedcase.SealedCaseRegistry;
import net.rai.scp_unleashed.sealedcase.SealedContents;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class StasisScans {
    public static final int ScanInterval = 20;
    public static final int SearchReach = 8;

    private StasisScans() {
    }

    public static boolean collect(ItemStack Piece, long Now, List<ItemStack> Into) {
        if (Piece.isEmpty()) {
            return false;
        }
        if (StasisPodItem.isExpired(Piece, Now)) {
            Into.add(Piece.copy());
            return true;
        }
        ItemContainerContents Box = Piece.get(DataComponents.CONTAINER);
        if (Box != null && Box.nonEmptyStream().anyMatch(Inner -> StasisPodItem.isExpired(Inner, Now))) {
            NonNullList<ItemStack> Slots = NonNullList.withSize(Box.getSlots(), ItemStack.EMPTY);
            Box.copyInto(Slots);
            for (int Index = 0; Index < Slots.size(); Index++) {
                if (StasisPodItem.isExpired(Slots.get(Index), Now)) {
                    Into.add(Slots.get(Index));
                    Slots.set(Index, ItemStack.EMPTY);
                }
            }
            Piece.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Slots));
        }
        SealedContents Sealed = Piece.get(SealedCaseRegistry.Contents.get());
        if (Sealed != null && StasisPodItem.isExpired(Sealed.stack(), Now)) {
            Into.add(Sealed.stack());
            Piece.remove(SealedCaseRegistry.Contents.get());
        }
        BundleContents Bundle = Piece.get(DataComponents.BUNDLE_CONTENTS);
        if (Bundle != null && Bundle.itemCopyStream().anyMatch(Inner -> StasisPodItem.isExpired(Inner, Now))) {
            List<ItemStack> Kept = new ArrayList<>();
            for (ItemStack Inner : Bundle.itemsCopy()) {
                if (StasisPodItem.isExpired(Inner, Now)) {
                    Into.add(Inner);
                } else {
                    Kept.add(Inner);
                }
            }
            Piece.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(Kept));
        }
        return false;
    }

    public static void scanPlayer(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        List<ItemStack> Found = new ArrayList<>();
        Inventory Bag = User.getInventory();
        for (int Index = 0; Index < Bag.getContainerSize(); Index++) {
            ItemStack Piece = Bag.getItem(Index);
            if (!Piece.isEmpty() && !StasisPodItem.isPod(Piece)) {
                int Before = Found.size();
                collect(Piece, Now, Found);
                if (Found.size() > Before) {
                    Bag.setChanged();
                }
            }
        }
        for (ItemStack Pod : Found) {
            StasisPods.breakOut(World, Pod, User.position(), User);
        }
    }

    public static void scanMenu(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        AbstractContainerMenu Menu = User.containerMenu;
        Map<Container, List<ItemStack>> Groups = new IdentityHashMap<>();
        for (Slot Entry : Menu.slots) {
            ItemStack Piece = Entry.getItem();
            if (Piece.isEmpty()) {
                continue;
            }
            List<ItemStack> Found = new ArrayList<>();
            if (collect(Piece, Now, Found)) {
                Entry.set(ItemStack.EMPTY);
            } else if (!Found.isEmpty()) {
                Entry.setChanged();
            }
            if (!Found.isEmpty()) {
                Groups.computeIfAbsent(Entry.container, Key -> new ArrayList<>()).addAll(Found);
            }
        }
        ItemStack Carried = Menu.getCarried();
        List<ItemStack> Held = new ArrayList<>();
        if (collect(Carried, Now, Held)) {
            Menu.setCarried(ItemStack.EMPTY);
        }
        if (!Held.isEmpty()) {
            Groups.computeIfAbsent(User.getInventory(), Key -> new ArrayList<>()).addAll(Held);
        }
        if (Groups.isEmpty()) {
            return;
        }
        Menu.broadcastChanges();
        for (Map.Entry<Container, List<ItemStack>> Group : Groups.entrySet()) {
            burst(World, User, Group.getKey(), Group.getValue());
        }
    }

    private static void burst(ServerLevel World, ServerPlayer User, Container Source, List<ItemStack> Pods) {
        if (Source instanceof Inventory) {
            for (ItemStack Pod : Pods) {
                StasisPods.breakOut(World, Pod, User.position(), User);
            }
            return;
        }
        List<BlockPos> Cells = cells(World, User, Source);
        Vec3 Centre;
        if (!Cells.isEmpty()) {
            if (User.containerMenu != User.inventoryMenu) {
                User.closeContainer();
            }
            for (BlockPos Cell : Cells) {
                World.destroyBlock(Cell, true);
            }
            Centre = Vec3.atBottomCenterOf(Cells.get(0));
        } else {
            Centre = anchor(World, User, Source);
        }
        for (ItemStack Pod : Pods) {
            StasisPods.breakOut(World, Pod, Centre, null);
        }
    }

    private static List<BlockPos> cells(ServerLevel World, ServerPlayer User, Container Source) {
        List<BlockPos> Found = new ArrayList<>();
        if (Source instanceof BlockEntity Holder && !Holder.isRemoved()) {
            Found.add(Holder.getBlockPos());
            BlockState State = Holder.getBlockState();
            if (State.getBlock() instanceof ChestBlock && State.hasProperty(ChestBlock.TYPE) && State.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                Found.add(Holder.getBlockPos().relative(ChestBlock.getConnectedDirection(State)));
            }
        } else if (Source instanceof CompoundContainer Compound) {
            Found.addAll(nearby(World, User.blockPosition(), Candidate -> Candidate instanceof Container Part && Compound.contains(Part)));
        }
        Found.removeIf(Pos -> World.getBlockState(Pos).getDestroySpeed(World, Pos) < 0.0f);
        return Found;
    }

    private static Vec3 anchor(ServerLevel World, ServerPlayer User, Container Source) {
        if (Source instanceof Entity Carrier) {
            return Carrier.position();
        }
        if (Source instanceof PlayerEnderChestContainer Ender) {
            List<BlockPos> Chests = nearby(World, User.blockPosition(), Candidate -> Candidate instanceof EnderChestBlockEntity Chest && Ender.isActiveChest(Chest));
            if (!Chests.isEmpty()) {
                return Vec3.atBottomCenterOf(Chests.get(0).above());
            }
        }
        return User.position();
    }

    private static List<BlockPos> nearby(ServerLevel World, BlockPos Around, Predicate<BlockEntity> Wanted) {
        List<BlockPos> Found = new ArrayList<>();
        ChunkPos Low = new ChunkPos(Around.offset(-SearchReach, 0, -SearchReach));
        ChunkPos High = new ChunkPos(Around.offset(SearchReach, 0, SearchReach));
        for (int ChunkX = Low.x; ChunkX <= High.x; ChunkX++) {
            for (int ChunkZ = Low.z; ChunkZ <= High.z; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Candidate : Chunk.getBlockEntities().values()) {
                    if (Candidate.getBlockPos().distSqr(Around) <= SearchReach * SearchReach && Wanted.test(Candidate)) {
                        Found.add(Candidate.getBlockPos());
                    }
                }
            }
        }
        return Found;
    }

    public static void scanCarrier(Entity Carrier) {
        if (!(Carrier.level() instanceof ServerLevel World)) {
            return;
        }
        long Now = World.getGameTime();
        if (Carrier instanceof LivingEntity Living) {
            for (EquipmentSlot Where : EquipmentSlot.values()) {
                ItemStack Piece = Living.getItemBySlot(Where);
                if (check(World, Piece, Now, Living)) {
                    Living.setItemSlot(Where, ItemStack.EMPTY);
                    StasisPods.breakOut(World, Piece, Living.position(), Living);
                }
            }
            if (Living instanceof InventoryCarrier Bearer) {
                Container Pack = Bearer.getInventory();
                for (int Index = 0; Index < Pack.getContainerSize(); Index++) {
                    ItemStack Piece = Pack.getItem(Index);
                    if (check(World, Piece, Now, Living)) {
                        Pack.setItem(Index, ItemStack.EMPTY);
                        StasisPods.breakOut(World, Piece, Living.position(), Living);
                    }
                }
            }
        } else if (Carrier instanceof ItemFrame Frame) {
            ItemStack Piece = Frame.getItem();
            if (check(World, Piece, Now, Frame)) {
                Frame.setItem(ItemStack.EMPTY);
                StasisPods.breakOut(World, Piece, Frame.position(), null);
            }
        }
    }

    private static boolean check(ServerLevel World, ItemStack Piece, long Now, Entity Carrier) {
        if (!(Piece.getItem() instanceof StasisPodItem Pod)) {
            return false;
        }
        StasisContents Held = StasisPodItem.contents(Piece);
        if (Held == null) {
            return false;
        }
        if (Held.expired(Now)) {
            return true;
        }
        long Left = Held.remaining(Now);
        if (Left <= StasisPodItem.WarningTicks) {
            StasisEffects.rattle(World, Carrier.getX(), Carrier.getY() + Carrier.getBbHeight() * 0.6, Carrier.getZ(), Pod.getGrade(), Left);
        }
        return false;
    }
}
