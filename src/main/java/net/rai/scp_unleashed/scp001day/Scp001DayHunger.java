package net.rai.scp_unleashed.scp001day;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Scp001DayHunger {
    public static final int CheckTicks = 100;
    public static final int GraceChecks = 6;
    public static final TagKey<Item> Keeps = TagKey.create(Registries.ITEM, Scp001DayRegistry.location("scp001_day_keeps"));
    private static final Map<UUID, Integer> Inside = new HashMap<>();

    private Scp001DayHunger() {
    }

    public static void forget(UUID Player) {
        Inside.remove(Player);
    }

    public static void tick(ServerLevel World, ServerPlayer User) {
        if (World.getGameTime() % CheckTicks != 0 || User.getAbilities().instabuild || User.isSpectator()) {
            return;
        }
        int Checks = Inside.merge(User.getUUID(), 1, Integer::sum);
        if (Checks < GraceChecks) {
            return;
        }
        if (spoil(User) > 0) {
            World.sendParticles(ParticleTypes.ASH, User.getX(), User.getY() + 1.0, User.getZ(), 24, 0.3, 0.4, 0.3, 0.02);
            World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.Crumble.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp001_day.food_crumbles").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static boolean perishable(ItemStack Stack, ServerPlayer User) {
        return !Stack.isEmpty() && Stack.getFoodProperties(User) != null && !Stack.is(Scp001DayRegistry.Tin.get()) && !Stack.is(Keeps);
    }

    public static int spoil(ServerPlayer User) {
        int Removed = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (perishable(Stack, User)) {
                Removed += Stack.getCount();
                User.getInventory().setItem(Slot, ItemStack.EMPTY);
            }
        }
        return Removed;
    }
}
