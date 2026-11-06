package net.rai.scp_unleashed.scp323;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp148.Scp148Shield;

import java.util.ArrayList;
import java.util.List;

public final class Scp323Possession {
    public static final String Prefix = "message.scp_unleashed.scp323.";
    public static final double DropSpread = 0.45;

    private Scp323Possession() {
    }

    public static InteractionResult touched(Scp323Skull Skull, Player User) {
        if (!(Skull.level() instanceof ServerLevel World) || !(User instanceof ServerPlayer Toucher)) {
            return InteractionResult.PASS;
        }
        if (Scp323Watch.isWatching(User)) {
            return InteractionResult.PASS;
        }
        if (User.isCreative() || !Scp323Night.isNight(World) || Scp323Sight.hidesFace(User) || Scp148Shield.blocks(User) || Skull.isQuiet() || Scp323Bond.isBound(User)) {
            ItemStack Taken = Skull.stack();
            if (!User.getInventory().add(Taken)) {
                User.displayClientMessage(Component.translatable(Prefix + "no_room").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            Skull.discard();
            World.playSound(null, Skull.getX(), Skull.getY(), Skull.getZ(), Scp323Registry.Lift.get(), SoundSource.PLAYERS, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
            return InteractionResult.CONSUME;
        }
        if (User.isSpectator() || Scp323Sight.wearsSkull(User)) {
            return InteractionResult.PASS;
        }
        ItemStack Taken = Skull.stack();
        Skull.discard();
        equip(Toucher, Taken);
        return InteractionResult.CONSUME;
    }

    public static void equip(ServerPlayer User, ItemStack Skull) {
        Scp323Carry.reset(User);
        if (Scp323Hosts.canHost(User)) {
            Scp323Hosts.fuse(User);
            Scp323Lure.sync(User);
            return;
        }
        ItemStack Old = User.getItemBySlot(EquipmentSlot.HEAD);
        if (!Old.isEmpty() && !User.getInventory().add(Old.copy())) {
            User.drop(Old.copy(), false);
        }
        User.setItemSlot(EquipmentSlot.HEAD, Skull.copyWithCount(1));
        User.getInventory().setChanged();
        Scp323Lure.sync(User);
    }

    public static void seize(ServerLevel World, Mob Person, ItemStack Skull) {
        if (!Scp323SkullItem.is(Skull)) {
            return;
        }
        Person.getNavigation().stop();
        Scp323Carry.reset(Person);
        if (Scp323Hosts.canHost(Person)) {
            Scp323Hosts.fuse(Person);
            return;
        }
        ItemStack Old = Person.getItemBySlot(EquipmentSlot.HEAD);
        if (!Old.isEmpty()) {
            Person.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Person.spawnAtLocation(Old.copy(), 1.6f);
        }
        Person.setItemSlot(EquipmentSlot.HEAD, Skull.copyWithCount(1));
        Person.setPersistenceRequired();
    }

    public static void carriedDeath(LivingEntity Victim) {
        if (!(Victim.level() instanceof ServerLevel World)) {
            return;
        }
        Scp323Carry.reset(Victim);
        if (!(Victim instanceof ServerPlayer User)) {
            ItemStack Worn = Victim.getItemBySlot(EquipmentSlot.HEAD);
            if (Scp323SkullItem.is(Worn)) {
                Victim.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                Scp323Skull.place(World, Victim.position(), Victim.getYRot(), Worn, true);
            }
            return;
        }
        List<ItemStack> Carried = takeAll(User);
        ItemStack Worn = User.getItemBySlot(EquipmentSlot.HEAD);
        if (Scp323SkullItem.is(Worn)) {
            User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Carried.add(Worn.copy());
        }
        Vec3 Spot = User.position();
        for (int Index = 0; Index < Carried.size(); Index++) {
            double Angle = Index * 2.39996;
            Vec3 Placed = Spot.add(Math.cos(Angle) * DropSpread, 0.25, Math.sin(Angle) * DropSpread);
            Scp323Skull.place(World, Placed, World.random.nextFloat() * 360.0f, Carried.get(Index), Index == 0);
        }
        Scp323Lure.resend(User);
    }

    public static List<ItemStack> takeAll(ServerPlayer User) {
        List<ItemStack> Found = new ArrayList<>();
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Scp323SkullItem.is(Stack)) {
                Found.add(Stack.copy());
                Pockets.setItem(Slot, ItemStack.EMPTY);
            }
        }
        ItemStack Cursor = User.containerMenu.getCarried();
        if (Scp323SkullItem.is(Cursor)) {
            Found.add(Cursor.copy());
            User.containerMenu.setCarried(ItemStack.EMPTY);
        }
        return Found;
    }
}
