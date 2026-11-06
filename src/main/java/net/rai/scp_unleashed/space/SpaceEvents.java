package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SpaceEvents {
    private SpaceEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SpaceRegistry.AirInfuserEntity.get(), (Machine, Side) -> Machine.getItemHandler(Side));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Equipment.getKey()) {
            for (SpaceSuitPiece Piece : SpaceSuitPiece.values()) {
                Event.accept(SpaceRegistry.suitItem(Piece).get());
            }
            Event.accept(SpaceAir.canister(0));
            Event.accept(SpaceAir.canister(AirCanisterItem.Capacity));
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(SpaceRegistry.AirInfuserItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            Event.accept(SpaceRegistry.LunarDustItem.get());
            Event.accept(SpaceRegistry.LunarRegolithItem.get());
            Event.accept(SpaceRegistry.LunarStoneItem.get());
            Event.accept(SpaceRegistry.LunarStoneSlabItem.get());
            Event.accept(SpaceRegistry.LunarStoneStairsItem.get());
            Event.accept(SpaceRegistry.LunarStoneWallItem.get());
            Event.accept(SpaceRegistry.PolishedLunarStoneItem.get());
            Event.accept(SpaceRegistry.LunarStoneBricksItem.get());
            Event.accept(SpaceRegistry.LunarStoneBrickSlabItem.get());
            Event.accept(SpaceRegistry.LunarStoneBrickStairsItem.get());
            Event.accept(SpaceRegistry.LunarStoneBrickWallItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        SpaceCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void settingSpawn(PlayerSetSpawnEvent Event) {
        Player User = Event.getEntity();
        if (Event.getNewSpawn() == null || User.getServer() == null) {
            return;
        }
        ServerLevel Target = User.getServer().getLevel(Event.getSpawnLevel());
        if (Target != null && SpaceAir.isAirless(Target)) {
            Event.setCanceled(true);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.space.no_spawn").withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static ItemStack fullCanister() {
        return SpaceAir.canister(AirCanisterItem.Capacity);
    }
}
