package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp143.client.Scp143ClientHooks;
import net.rai.scp_unleashed.scp143.network.Scp143CutPayload;
import net.rai.scp_unleashed.scp143.network.Scp143PetalPayload;
import net.rai.scp_unleashed.scp143.network.Scp143SitePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp143Events {
    private Scp143Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp143PetalPayload.PayloadType, Scp143PetalPayload.PayloadCodec, (Payload, Context) -> Scp143ClientHooks.petals(Payload));
        Registrar.playToClient(Scp143CutPayload.PayloadType, Scp143CutPayload.PayloadCodec, (Payload, Context) -> Scp143ClientHooks.cut(Payload));
        Registrar.playToClient(Scp143SitePayload.PayloadType, Scp143SitePayload.PayloadCodec, (Payload, Context) -> Scp143ClientHooks.site(Payload));
    }

    @SubscribeEvent
    public static void addSignBlocks(BlockEntityTypeAddBlocksEvent Event) {
        Event.modify(BlockEntityType.SIGN, Scp143Registry.Sign.get(), Scp143Registry.WallSign.get());
        Event.modify(BlockEntityType.HANGING_SIGN, Scp143Registry.HangingSign.get(), Scp143Registry.WallHangingSign.get());
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent Event) {
        Event.enqueueWork(() -> {
            FireBlock Fire = (FireBlock) Blocks.FIRE;
            Fire.setFlammable(Scp143Registry.Log.get(), 5, 5);
            Fire.setFlammable(Scp143Registry.Wood.get(), 5, 5);
            Fire.setFlammable(Scp143Registry.StrippedLog.get(), 5, 5);
            Fire.setFlammable(Scp143Registry.StrippedWood.get(), 5, 5);
            Fire.setFlammable(Scp143Registry.Planks.get(), 5, 20);
            Fire.setFlammable(Scp143Registry.Stairs.get(), 5, 20);
            Fire.setFlammable(Scp143Registry.Slab.get(), 5, 20);
            Fire.setFlammable(Scp143Registry.Fence.get(), 5, 20);
            Fire.setFlammable(Scp143Registry.FenceGate.get(), 5, 20);
            Fire.setFlammable(Scp143Registry.Leaves.get(), 30, 60);
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(Scp143Registry.Sapling.getId(), Scp143Registry.PottedSapling);
            DispenserBlock.registerBehavior(Scp143Registry.BladewoodBoat.get(), new Scp143BoatItem.Dispense());
            DispenserBlock.registerBehavior(Scp143Registry.BladewoodChestBoat.get(), new Scp143BoatItem.Dispense());
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp143Registry.LogItem.get());
            Event.accept(Scp143Registry.WoodItem.get());
            Event.accept(Scp143Registry.StrippedLogItem.get());
            Event.accept(Scp143Registry.StrippedWoodItem.get());
            Event.accept(Scp143Registry.PlanksItem.get());
            Event.accept(Scp143Registry.StairsItem.get());
            Event.accept(Scp143Registry.SlabItem.get());
            Event.accept(Scp143Registry.FenceItem.get());
            Event.accept(Scp143Registry.FenceGateItem.get());
            Event.accept(Scp143Registry.DoorItem.get());
            Event.accept(Scp143Registry.TrapdoorItem.get());
            Event.accept(Scp143Registry.PressurePlateItem.get());
            Event.accept(Scp143Registry.ButtonItem.get());
            Event.accept(Scp143Registry.BladewoodSign.get());
            Event.accept(Scp143Registry.BladewoodHangingSign.get());
            Event.accept(Scp143Registry.LeavesItem.get());
            Event.accept(Scp143Registry.SaplingItem.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp143Registry.BladewoodBoat.get());
            Event.accept(Scp143Registry.BladewoodChestBoat.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp143Sites.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp143Petals.tick(World);
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent Event) {
        Scp143GroveCondition.retireContacts(Event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent Event) {
        Scp143Petals.clear();
    }

    @SubscribeEvent
    public static void knock(PlayerInteractEvent.LeftClickBlock Event) {
        if (Event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START || !(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        BlockPos Pos = Event.getPos();
        if (Scp143Registry.isBladewood(World.getBlockState(Pos).getBlock())) {
            Scp143Petals.knock(World, Pos, World.getRandom());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            sendSite(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            sendSite(User);
            Scp143Petals.resend(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp143Petals.resend(User);
        }
    }

    public static void sendSite(ServerPlayer User) {
        BlockPos Base = Scp143World.get(User.server).base();
        boolean Known = Base != null && User.serverLevel().dimension() == Level.OVERWORLD;
        PacketDistributor.sendToPlayer(User, new Scp143SitePayload(Known, Known ? Base : BlockPos.ZERO));
    }
}
