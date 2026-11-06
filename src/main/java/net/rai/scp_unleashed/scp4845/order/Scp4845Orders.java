package net.rai.scp_unleashed.scp4845.order;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.Scp4845Restaurant;
import net.rai.scp_unleashed.scp4845.Scp4845Site;
import net.rai.scp_unleashed.scp4845.block.Scp4845CounterBlock;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Lines;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class Scp4845Orders {
    public static final double ServeRange = 6.0;
    public static final int HandOverTicks = 32;
    public static final int DeliverDelay = 20;
    public static final int CounterSearch = 3;
    private static final List<Delivery> Pending = new ArrayList<>();

    private record Delivery(UUID Customer, ServerLevel World, BlockPos Counter, ItemStack Goods, long Due) {
    }

    private Scp4845Orders() {
    }

    @Nullable
    public static Scp4845Employee server(ServerLevel World, BlockPos Counter) {
        Scp4845Employee Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Scp4845Employee Worker : World.getEntitiesOfClass(Scp4845Employee.class, new AABB(Counter).inflate(ServeRange), Worker -> Worker.isAlive() && !Worker.isManager())) {
            double Distance = Worker.distanceToSqr(Vec3.atCenterOf(Counter));
            boolean Preferred = Worker.role() == Scp4845Employee.RoleCounter;
            if (Preferred) {
                Distance -= 1000.0;
            }
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Worker;
            }
        }
        return Best;
    }

    public static boolean openAt(ServerPlayer Customer, BlockPos Counter) {
        ServerLevel World = Customer.serverLevel();
        Scp4845Employee Worker = server(World, Counter);
        if (Worker == null) {
            Customer.displayClientMessage(Component.translatable("message.scp_unleashed.scp4845.nobody").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return true;
        }
        BlockPos Anchor = Counter.immutable();
        Customer.openMenu(new SimpleMenuProvider((Id, Pockets, User) -> new Scp4845OrderMenu(Id, Pockets, Anchor), Component.translatable("gui.scp_unleashed.scp4845_order.title")),
                Buffer -> Buffer.writeBlockPos(Anchor));
        Scp4845Registry.playLayers(World, null, Anchor.getX() + 0.5, Anchor.getY() + 1.1, Anchor.getZ() + 0.5, List.of(Scp4845Registry.BeepClick), SoundSource.BLOCKS, 0.5f, 1.2f);
        return true;
    }

    @Nullable
    private static BlockPos nearestCounter(ServerLevel World, Scp4845Employee Worker) {
        Scp4845Site Site = Scp4845Restaurant.in(World);
        if (Site != null && Worker.isBound() && Worker.role() == Scp4845Employee.RoleCounter) {
            return Site.CounterWorker.Pos().relative(Site.CounterWorker.Facing());
        }
        BlockPos Origin = Worker.blockPosition();
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Spot : BlockPos.betweenClosed(Origin.offset(-CounterSearch, -1, -CounterSearch), Origin.offset(CounterSearch, 1, CounterSearch))) {
            if (World.getBlockState(Spot).getBlock() instanceof Scp4845CounterBlock) {
                double Distance = Spot.distSqr(Origin);
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Spot.immutable();
                }
            }
        }
        return Best;
    }

    public static boolean interact(Scp4845Employee Worker, ServerPlayer Customer) {
        ServerLevel World = Customer.serverLevel();
        if (!Worker.isManager() && (Worker.role() == Scp4845Employee.RoleCounter || !Worker.isBound())) {
            BlockPos Counter = nearestCounter(World, Worker);
            if (Counter != null) {
                return openAt(Customer, Counter);
            }
        }
        Worker.brain().notice(Customer);
        Scp4845Lines.say(Worker, Customer, Scp4845Lines.categoryOf(Worker));
        return true;
    }

    public static int emeralds(Inventory Pockets) {
        int Count = 0;
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.is(Items.EMERALD)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    private static void pay(Inventory Pockets, int Cost) {
        int Left = Cost;
        for (int Slot = 0; Slot < Pockets.getContainerSize() && Left > 0; Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.is(Items.EMERALD)) {
                int Taken = Math.min(Left, Stack.getCount());
                Stack.shrink(Taken);
                Left -= Taken;
            }
        }
        Pockets.setChanged();
    }

    public static boolean order(ServerPlayer Customer, BlockPos Counter, Scp4845MenuItem Entry, int Quantity) {
        ServerLevel World = Customer.serverLevel();
        int Cost = Entry.Price * Quantity;
        if (!Customer.getAbilities().instabuild && emeralds(Customer.getInventory()) < Cost) {
            Customer.displayClientMessage(Component.translatable("message.scp_unleashed.scp4845.short", Cost).withStyle(ChatFormatting.RED), true);
            return false;
        }
        Scp4845Employee Worker = server(World, Counter);
        if (Worker == null) {
            Customer.displayClientMessage(Component.translatable("message.scp_unleashed.scp4845.nobody").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        if (!Customer.getAbilities().instabuild) {
            pay(Customer.getInventory(), Cost);
        }
        Worker.handOver(HandOverTicks);
        long Due = World.getGameTime() + DeliverDelay;
        int Total = Entry.Count * Quantity;
        while (Total > 0) {
            int Batch = Math.min(Total, Scp4845MenuItem.StackSize);
            Pending.add(new Delivery(Customer.getUUID(), World, Counter.immutable(), new ItemStack(Scp4845Registry.food(Entry), Batch), Due));
            Total -= Batch;
        }
        double X = Counter.getX() + 0.5;
        double Y = Counter.getY() + 1.1;
        double Z = Counter.getZ() + 0.5;
        Scp4845Registry.playLayers(World, null, X, Y, Z, Scp4845Registry.BeepLayers, SoundSource.BLOCKS, 0.7f, 1.0f);
        Scp4845Registry.playLayers(World, null, X, Y, Z, Scp4845Registry.DrawerLayers, SoundSource.BLOCKS, 0.6f, 1.0f);
        if (Entry == Scp4845MenuItem.LunarBlast) {
            Scp4845Registry.playLayers(World, null, X, Y, Z, Scp4845Registry.PourLayers, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return true;
    }

    public static int pending() {
        return Pending.size();
    }

    public static void tick(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        Iterator<Delivery> Walker = Pending.iterator();
        while (Walker.hasNext()) {
            Delivery Next = Walker.next();
            if (Next.World().getGameTime() < Next.Due()) {
                continue;
            }
            Walker.remove();
            ServerPlayer Customer = Next.World().getPlayerByUUID(Next.Customer()) instanceof ServerPlayer Found ? Found : Server.getPlayerList().getPlayer(Next.Customer());
            if (Customer == null || Customer.isRemoved() || Customer.serverLevel() != Next.World()) {
                Vec3 Spot = Vec3.atCenterOf(Next.Counter()).add(0.0, 0.7, 0.0);
                Next.World().addFreshEntity(new ItemEntity(Next.World(), Spot.x, Spot.y, Spot.z, Next.Goods()));
                continue;
            }
            ItemStack Goods = Next.Goods().copy();
            if (!Customer.getInventory().add(Goods) && !Goods.isEmpty()) {
                Customer.drop(Goods, false);
            }
            Scp4845Registry.playLayers(Next.World(), null, Customer.getX(), Customer.getY() + 1.0, Customer.getZ(), Scp4845Registry.BagLayers, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    public static void clear() {
        Pending.clear();
    }
}
