package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.gear.GearSounds;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SpaceAir {
    public static final EquipmentSlot[] SuitSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final List<Shelter> Shelters = new CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface Shelter {
        boolean shelters(LivingEntity Breather);
    }

    private SpaceAir() {
    }

    public static boolean isAirless(Level World) {
        return World.dimensionTypeRegistration().is(SpaceTags.Airless);
    }

    public static void registerShelter(Shelter Hook) {
        Shelters.add(Hook);
    }

    public static boolean sheltered(LivingEntity Breather) {
        for (Shelter Hook : Shelters) {
            if (Hook.shelters(Breather)) {
                return true;
            }
        }
        return false;
    }

    public static int canisterAir(ItemStack Stack) {
        return Stack.getItem() instanceof AirCanisterItem ? Math.min(AirCanisterItem.Capacity, Stack.getOrDefault(SpaceRegistry.Air.get(), 0)) : 0;
    }

    public static ItemStack canister(int Amount) {
        ItemStack Stack = new ItemStack(SpaceRegistry.AirCanister.get());
        Stack.set(SpaceRegistry.Air.get(), Math.max(0, Math.min(AirCanisterItem.Capacity, Amount)));
        return Stack;
    }

    public static SpaceSuitTank tank(ItemStack Chest) {
        return Chest.getOrDefault(SpaceRegistry.SuitTank.get(), SpaceSuitTank.Unfitted);
    }

    public static boolean hasTank(ItemStack Chest) {
        return Chest.getItem() instanceof SpaceSuitItem Suit && Suit.getPiece() == SpaceSuitPiece.Chestplate;
    }

    public static boolean wearsFullSuit(LivingEntity Wearer) {
        for (EquipmentSlot Slot : SuitSlots) {
            if (!Wearer.getItemBySlot(Slot).is(SpaceTags.SpaceSuit)) {
                return false;
            }
        }
        return hasTank(Wearer.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static int suitAir(LivingEntity Wearer) {
        ItemStack Chest = Wearer.getItemBySlot(EquipmentSlot.CHEST);
        return hasTank(Chest) ? tank(Chest).Air() : 0;
    }

    public static boolean breathing(LivingEntity Wearer) {
        return wearsFullSuit(Wearer) && suitAir(Wearer) > 0;
    }

    public static boolean inPressurizedVehicle(Entity Rider) {
        Entity Vehicle = Rider.getVehicle();
        while (Vehicle != null) {
            if (Vehicle.getType().is(SpaceTags.PressurizedVehicles)) {
                return true;
            }
            Vehicle = Vehicle.getVehicle();
        }
        return false;
    }

    public static boolean drain(LivingEntity Wearer, int Amount) {
        ItemStack Chest = Wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (!hasTank(Chest)) {
            return false;
        }
        SpaceSuitTank Current = tank(Chest);
        if (Current.Air() <= 0) {
            return false;
        }
        Chest.set(SpaceRegistry.SuitTank.get(), Current.withAir(Current.Air() - Amount));
        return true;
    }

    public static String problem(ItemStack Chest, ItemStack Held) {
        if (!hasTank(Chest)) {
            return "wear_suit";
        }
        int Offered = canisterAir(Held);
        if (Offered <= 0) {
            return "canister_empty";
        }
        SpaceSuitTank Current = tank(Chest);
        return Current.Fitted() && Current.Air() >= Offered ? "tank_fuller" : null;
    }

    public static ItemStack fit(ItemStack Chest, ItemStack Held) {
        SpaceSuitTank Current = tank(Chest);
        Chest.set(SpaceRegistry.SuitTank.get(), new SpaceSuitTank(true, canisterAir(Held)));
        return Current.Fitted() ? canister(Current.Air()) : ItemStack.EMPTY;
    }

    public static boolean refill(Player User, ItemStack Held) {
        ItemStack Chest = User.getItemBySlot(EquipmentSlot.CHEST);
        String Problem = problem(Chest, Held);
        if (Problem != null) {
            if (!User.level().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.space." + Problem).withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        if (User.level().isClientSide) {
            return true;
        }
        ItemStack Returned = fit(Chest, Held.copyWithCount(1));
        Held.consume(1, User);
        if (!Returned.isEmpty() && !User.getInventory().add(Returned)) {
            User.drop(Returned, false);
        }
        refillSound(User, 1.0F);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.space.tank_fitted").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static void refillSound(Player User, float Volume) {
        User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), SpaceRegistry.SuitRefill.get(), SoundSource.PLAYERS, Volume, 1.0F);
        User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), GearSounds.HazmatClasp.get(), SoundSource.PLAYERS, Volume * 0.8F, 1.1F);
    }
}
