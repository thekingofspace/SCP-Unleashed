package net.rai.scp_unleashed.anomaly;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.curios.CuriosDrops;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.sealedcase.SealedCaseItem;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.storage.StorageTags;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class AnomalyDrops {
    public static final String RuleName = "scpDropAnomalies";

    public static GameRules.Key<GameRules.BooleanValue> DropAnomalies;

    private AnomalyDrops() {
    }

    public static void register(IEventBus ModEventBus) {
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(AnomalyDrops::registerRule));
    }

    private static void registerRule() {
        if (DropAnomalies == null) {
            DropAnomalies = GameRules.register(RuleName, GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
        }
    }

    public static boolean isAnomalous(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        return StorageTags.isStorable(Stack) || StasisPodItem.isOccupied(Stack) || SealedCaseItem.isSealed(Stack) || LabVials.carriesCulture(Stack);
    }

    public static boolean shrugsOff(ItemStack Stack, DamageSource Source) {
        return isAnomalous(Stack) && (Source.is(DamageTypes.CACTUS) || Source.is(DamageTypeTags.IS_FIRE));
    }

    @SubscribeEvent
    public static void dropped(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide() && Event.getEntity() instanceof ItemEntity Dropped && isAnomalous(Dropped.getItem())) {
            Dropped.setUnlimitedLifetime();
        }
    }

    public static boolean active(ServerPlayer User) {
        GameRules Rules = User.serverLevel().getGameRules();
        return DropAnomalies != null && Rules.getBoolean(GameRules.RULE_KEEPINVENTORY) && Rules.getBoolean(DropAnomalies);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || User.isSpectator() || !active(User)) {
            return;
        }
        Inventory Pockets = User.getInventory();
        List<ItemStack> Packed = new ArrayList<>();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (isAnomalous(Stack)) {
                Pockets.setItem(Slot, ItemStack.EMPTY);
                User.drop(Stack, true, false);
            } else if (PackedAnomalies.unpack(Stack, Packed)) {
                Pockets.setChanged();
            }
        }
        if (Senses.curiosLoaded()) {
            CuriosDrops.drop(User, Packed);
        }
        BackpackedBags.unpack(User, Packed);
        for (ItemStack Stack : Packed) {
            User.drop(Stack, true, false);
        }
    }
}
