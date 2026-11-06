package net.rai.scp_unleashed.scp035;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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

public final class Scp035Possession {
    public static final String Prefix = "message.scp_unleashed.scp035.";
    public static final double DropSpread = 0.45;

    private Scp035Possession() {
    }

    public static InteractionResult touched(Scp035Mask Mask, Player User) {
        if (!(Mask.level() instanceof ServerLevel World) || !(User instanceof ServerPlayer Toucher)) {
            return InteractionResult.PASS;
        }
        if (User.isCreative() || Scp035Sight.hidesFace(User) || Scp148Shield.blocks(User)) {
            ItemStack Taken = Mask.stack();
            if (!User.getInventory().add(Taken)) {
                User.displayClientMessage(Component.translatable(Prefix + "no_room").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            Mask.discard();
            World.playSound(null, Mask.getX(), Mask.getY(), Mask.getZ(), Scp035Registry.Lift.get(), SoundSource.PLAYERS, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
            return InteractionResult.CONSUME;
        }
        if (User.isSpectator() || Scp035Sight.wearsMask(User)) {
            return InteractionResult.PASS;
        }
        ItemStack Taken = Mask.stack();
        Mask.discard();
        equip(Toucher, Taken);
        return InteractionResult.CONSUME;
    }

    public static void equip(ServerPlayer User, ItemStack Mask) {
        ServerLevel World = User.serverLevel();
        ItemStack Old = User.getItemBySlot(EquipmentSlot.HEAD);
        User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        if (!Old.isEmpty()) {
            ItemStack Moved = Old.copy();
            if (!User.getInventory().add(Moved) || !Moved.isEmpty()) {
                crack(World, User, Old);
                User.displayClientMessage(Component.translatable(Prefix + "helmet_destroyed", Old.getHoverName()).withStyle(ChatFormatting.DARK_RED), true);
            }
        }
        User.setItemSlot(EquipmentSlot.HEAD, Mask.copyWithCount(1));
        User.getInventory().setChanged();
        latched(World, User);
        Scp035Carry.reset(User);
        Scp035Lure.sync(User);
    }

    public static void seize(ServerLevel World, Mob Person, ItemStack Mask) {
        if (!Scp035MaskItem.is(Mask)) {
            return;
        }
        ItemStack Old = Person.getItemBySlot(EquipmentSlot.HEAD);
        if (!Old.isEmpty()) {
            Person.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Person.spawnAtLocation(Old.copy(), 1.6f);
        }
        Person.setItemSlot(EquipmentSlot.HEAD, Mask.copyWithCount(1));
        Person.setPersistenceRequired();
        Person.getNavigation().stop();
        Scp035Carry.reset(Person);
        latched(World, Person);
    }

    private static void crack(ServerLevel World, LivingEntity Wearer, ItemStack Broken) {
        Vec3 Head = Wearer.getEyePosition();
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Broken), Head.x, Head.y + 0.1, Head.z, 14, 0.18, 0.18, 0.18, 0.08);
        World.playSound(null, Head.x, Head.y, Head.z, Scp035Registry.HelmetCrack.get(), SoundSource.PLAYERS, 1.0f, 0.9f + World.random.nextFloat() * 0.15f);
    }

    public static void latched(ServerLevel World, LivingEntity Wearer) {
        Vec3 Head = Wearer.getEyePosition();
        World.playSound(null, Head.x, Head.y, Head.z, Scp035Registry.Latch.get(), SoundSource.PLAYERS, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        World.playSound(null, Head.x, Head.y, Head.z, Scp035Registry.LatchVoice.get(), SoundSource.HOSTILE, 0.7f, 0.9f + World.random.nextFloat() * 0.1f);
        World.sendParticles(Scp035Registry.Weep.get(), Head.x, Head.y, Head.z, 6, 0.15, 0.1, 0.15, 0.0);
    }

    public static void died(LivingEntity Victim) {
        if (!(Victim.level() instanceof ServerLevel World)) {
            return;
        }
        if (Victim instanceof ServerPlayer User) {
            playerDied(World, User);
            return;
        }
        Scp035Carry.reset(Victim);
        ItemStack Worn = Victim.getItemBySlot(EquipmentSlot.HEAD);
        if (!Scp035MaskItem.is(Worn)) {
            return;
        }
        Victim.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        if (!(Victim instanceof Scp035Host) && Victim instanceof Mob Person && Scp035Sight.isPerson(Person)) {
            Scp035Host.rise(World, Person, Worn, false);
            return;
        }
        Scp035Mask.place(World, Victim.position(), Victim.getYRot(), Worn, true);
    }

    private static void playerDied(ServerLevel World, ServerPlayer User) {
        ItemStack Worn = User.getItemBySlot(EquipmentSlot.HEAD);
        boolean Masked = Scp035MaskItem.is(Worn);
        if (Masked) {
            User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        List<ItemStack> Carried = takeAll(User);
        Vec3 Spot = User.position();
        for (int Index = 0; Index < Carried.size(); Index++) {
            double Angle = Index * 2.39996;
            Vec3 Placed = Spot.add(Math.cos(Angle) * DropSpread, 0.25, Math.sin(Angle) * DropSpread);
            Scp035Mask.place(World, Placed, World.random.nextFloat() * 360.0f, Carried.get(Index), Index == 0);
        }
        if (Masked) {
            Scp035Host.rise(World, User, Worn);
        }
        Scp035Carry.reset(User);
        Scp035Lure.resend(User);
    }

    public static List<ItemStack> takeAll(ServerPlayer User) {
        List<ItemStack> Found = new ArrayList<>();
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Scp035MaskItem.is(Stack)) {
                Found.add(Stack.copy());
                Pockets.setItem(Slot, ItemStack.EMPTY);
            }
        }
        ItemStack Cursor = User.containerMenu.getCarried();
        if (Scp035MaskItem.is(Cursor)) {
            Found.add(Cursor.copy());
            User.containerMenu.setCarried(ItemStack.EMPTY);
        }
        return Found;
    }
}
