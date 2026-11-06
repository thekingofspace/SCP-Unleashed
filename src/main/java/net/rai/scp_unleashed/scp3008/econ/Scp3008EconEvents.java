package net.rai.scp_unleashed.scp3008.econ;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008StoreArmorItem;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008CrowbarItem;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008EconEvents {
    private Scp3008EconEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            accept(Event, Scp3008EconRegistry.Materials);
            accept(Event, Scp3008EconRegistry.Survival);
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            accept(Event, Scp3008EconRegistry.Placeables);
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            accept(Event, Scp3008EconRegistry.Foods);
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent Event, List<DeferredItem<? extends Item>> Entries) {
        for (DeferredItem<? extends Item> Entry : Entries) {
            Event.accept(Entry.get());
        }
    }

    public static boolean disassembling(Player User) {
        return User != null && User.getMainHandItem().is(Scp3008EconRegistry.DisassemblyTools);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakFurniture(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (!(Event.getLevel() instanceof Level World) || World.isClientSide || !disassembling(User) || !Scp3008Carry.liftable(Event.getState())
                || Scp3008Carry.carrying(User)) {
            return;
        }
        ItemStack Tool = User.getMainHandItem();
        if (Scp3008Disassembly.unpack(User, Event.getPos())) {
            Event.setCanceled(true);
            Tool.hurtAndBreak(1, User, EquipmentSlot.MAINHAND);
        }
    }

    @SubscribeEvent
    public static void offhandLift(PlayerInteractEvent.RightClickBlock Event) {
        if (Event.getHand() == InteractionHand.OFF_HAND && disassembling(Event.getEntity()) && Event.getItemStack().getItem() instanceof Scp3008CrowbarItem) {
            Event.setCancellationResult(InteractionResult.PASS);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void staffNotice(LivingChangeTargetEvent Event) {
        if (!(Event.getEntity() instanceof Scp3008Staff Staff) || !(Event.getNewAboutToBeSetTarget() instanceof LivingEntity Target) || Staff.provokedBy(Target)
                || !Scp3008StoreArmorItem.fullSet(Target)) {
            return;
        }
        double Range = Scp3008Staff.FollowRange * Scp3008StoreArmorItem.NoticeShare;
        if (Target.distanceToSqr(Staff) > Range * Range) {
            Event.setCanceled(true);
        }
    }
}
