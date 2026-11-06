package net.rai.scp_unleashed.scp063;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp063Events {
    private Scp063Events() {
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        if (!Scp063ToothbrushItem.isBrushingTeeth(User)) {
            Scp063Brushing.rest(User);
        }
        if (!Scp063Scrubbing.scrubbingNow(User)) {
            Scp063Scrubbing.stop(User);
            if (Scp063Scrubbing.isScrubbing(User) && !Scp063ToothbrushItem.isBrushing(User)) {
                Scp063Scrubbing.mark(User, false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (wouldChew(User, Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            if (User instanceof ServerPlayer Server) {
                Scp063Teeth.refuse(Server);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void startUsing(LivingEntityUseItemEvent.Start Event) {
        if (Event.getEntity() instanceof Player User && wouldChew(User, Event.getItem())) {
            Event.setCanceled(true);
            if (User instanceof ServerPlayer Server) {
                Scp063Teeth.refuse(Server);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        BlockState State = Event.getLevel().getBlockState(Event.getPos());
        if (!Scp063Teeth.isCake(State) || !Scp063Teeth.isToothless(User) || !User.canEat(false) || decorating(State, Event.getItemStack())) {
            return;
        }
        Event.setUseBlock(TriState.FALSE);
        if (User instanceof ServerPlayer Server) {
            Scp063Teeth.refuse(Server);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp063Registry.Toothbrush.get());
        }
    }

    private static boolean wouldChew(Player User, ItemStack Stack) {
        if (!Scp063Teeth.isChewing(Stack) || !Scp063Teeth.isToothless(User)) {
            return false;
        }
        FoodProperties Food = Stack.getFoodProperties(User);
        return Food == null || User.canEat(Food.canAlwaysEat());
    }

    private static boolean decorating(BlockState State, ItemStack Held) {
        return State.getBlock() instanceof CakeBlock && State.getValue(CakeBlock.BITES) == 0 && Held.is(ItemTags.CANDLES);
    }
}
