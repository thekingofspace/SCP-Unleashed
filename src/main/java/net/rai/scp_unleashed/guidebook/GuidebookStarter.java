package net.rai.scp_unleashed.guidebook;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GuidebookStarter {
    public static final String GivenKey = "scp_unleashed_facility_tablet_given";

    private GuidebookStarter() {
    }

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        CompoundTag Root = User.getPersistentData();
        CompoundTag Kept = Root.getCompound(Player.PERSISTED_NBT_TAG);
        if (Kept.getBoolean(GivenKey)) {
            return;
        }
        ItemStack Tablet = new ItemStack(GuidebookRegistry.FacilityTablet.get());
        if (!User.getInventory().add(Tablet)) {
            User.drop(Tablet, false);
        }
        Kept.putBoolean(GivenKey, true);
        Root.put(Player.PERSISTED_NBT_TAG, Kept);
    }
}
