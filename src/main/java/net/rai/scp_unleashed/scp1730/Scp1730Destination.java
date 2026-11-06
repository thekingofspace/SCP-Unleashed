package net.rai.scp_unleashed.scp1730;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;

public final class Scp1730Destination {
    public static final ResourceLocation Id = Scp1730Registry.location("scp1730_facility");
    public static final ResourceLocation StandinId = Scp1730Registry.location("scp1730_facility_standin");
    public static final int Tint = 0xB8C4D2;
    public static final int ShardVariant = 3;
    public static final int WarningLines = 3;
    public static final String SealedKey = "scp_unleashed_scp1730_sealed";
    public static final String WitnessKey = "scp_unleashed_scp1730_witness";

    private Scp1730Destination() {
    }

    public static GatewayDestination make() {
        return make(Id, Scp1730World.Key);
    }

    public static GatewayDestination make(ResourceLocation Name, ResourceKey<Level> Dimension) {
        return new GatewayDestination(Name, Dimension, Tint, ShardVariant, WarningLines, Scp1730Destination::entry, GatewayDestination.OpenTravel, new Facility());
    }

    public static GatewayDestination standIn(ResourceKey<Level> Dimension) {
        GatewayDestination Found = GatewayDestinations.get(StandinId);
        if (Found == null) {
            Found = GatewayDestinations.register(make(StandinId, Dimension));
        }
        Scp1730World.standIn(Found.Dimension());
        return Found;
    }

    private static GatewayDestination.Arrival entry(ServerLevel Target, ServerPlayer User) {
        Scp1730State State = Scp1730State.get(Target.getServer());
        int Run = Math.max(0, State.run());
        Scp1730Plan Plan = Scp1730World.plan(Run);
        Vec3 Spot = Scp1730World.world(Run, Plan.arrival());
        return new GatewayDestination.Arrival(Spot, Plan.arrival().Yaw());
    }

    public static boolean sealed(Player User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Persisted.getBoolean(SealedKey);
    }

    public static void seal(Player User) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.putBoolean(SealedKey, true);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static void unseal(Player User) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.remove(SealedKey);
        Persisted.remove(WitnessKey);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static void witness(Player User, int Run) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.putInt(WitnessKey, Run + 1);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static boolean witnessed(Player User) {
        return User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(WitnessKey) > 0;
    }

    public static void giveFragment(ServerPlayer User) {
        stripFragments(User);
        ItemStack Fresh = new ItemStack(Scp1730Registry.Fragment.get());
        if (!User.getInventory().add(Fresh)) {
            User.drop(Fresh, false);
        }
    }

    public static int stripFragments(Player User) {
        int Removed = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.is(Scp1730Registry.Fragment.get())) {
                Removed += Stack.getCount();
                User.getInventory().setItem(Slot, ItemStack.EMPTY);
            }
        }
        if (User.containerMenu != null && User.containerMenu.getCarried().is(Scp1730Registry.Fragment.get())) {
            User.containerMenu.setCarried(ItemStack.EMPTY);
            Removed++;
        }
        return Removed;
    }

    private static final class Facility implements GatewayDestination.Hooks {
        @Override
        public boolean canEnter(ServerPlayer User, ServerLevel Target) {
            if (sealed(User) || witnessed(User)) {
                seal(User);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.sealed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return false;
            }
            Scp1730State State = Scp1730State.get(User.server);
            if (State.active() && State.collapsing()) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.collapsing").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return false;
            }
            return true;
        }

        @Override
        public void prepare(ServerLevel Target) {
            Scp1730Director.prepare(Target);
        }

        @Override
        public void arrived(ServerPlayer User, ServerLevel Target) {
            giveFragment(User);
            Scp1730Symbols.release(User);
            Target.playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.AmbienceAlarm.get(), SoundSource.AMBIENT, 0.6f, 0.8f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.arrive").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }

        @Override
        public void left(ServerPlayer User, ServerLevel From) {
            stripFragments(User);
            Scp1730Symbols.release(User);
            Scp1730State State = Scp1730State.get(User.server);
            if (State.active() && State.collapsing() || witnessed(User)) {
                seal(User);
            }
        }
    }
}
