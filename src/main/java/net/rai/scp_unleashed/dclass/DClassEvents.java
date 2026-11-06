package net.rai.scp_unleashed.dclass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.client.DClassClientHooks;
import net.rai.scp_unleashed.dclass.network.OpenPersonnelFilePayload;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp016.Scp016Spawn;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DClassEvents {
    private DClassEvents() {
    }

    @SubscribeEvent
    public static void keepSleeping(CanContinueSleepingEvent Event) {
        if (Event.getEntity() instanceof DClassLike Sleeper && Event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE
                && DClassBeds.sleepingSpot(Sleeper) != null) {
            Event.setContinueSleeping(true);
        }
    }

    @SubscribeEvent
    public static void noticeBed(BlockEvent.NeighborNotifyEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getState().is(DClassBeds.Beds)) {
            DClassBeds.forget(World, Event.getPos());
        }
    }

    @SubscribeEvent
    public static void forgetBeds(ServerStoppedEvent Event) {
        DClassBeds.clearAll();
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(DClassRegistry.DClassType.get(), DClass.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(OpenPersonnelFilePayload.PayloadType, OpenPersonnelFilePayload.PayloadCodec, (Payload, Context) -> DClassClientHooks.openFile(Payload));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Entity, Side) -> {
            CloningMachineBlockEntity Tank = CloningMachineBlock.masterEntity(World, Pos, State);
            return Tank == null ? null : Tank.access();
        }, DClassRegistry.CloningMachine.get());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        DClassCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(DClassRegistry.CloningMachineItem.get());
            Event.accept(DClassRegistry.Blood.get());
            Event.accept(DClassRegistry.BlankFile.get());
            Event.accept(DClassRegistry.DClassEgg.get());
        }
    }

    @SubscribeEvent
    public static void huntPersonnel(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (Event.getEntity() instanceof Zombie Hunter) {
            Hunter.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(Hunter, DClassLike.class, 10, false, false, ControlBodies::isPersonLike));
        } else if (Event.getEntity() instanceof AbstractIllager Raider) {
            Raider.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(Raider, DClassLike.class, 10, false, false, ControlBodies::isPersonLike));
        } else if (Event.getEntity() instanceof Scp016Spawn Spawn) {
            Spawn.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(Spawn, DClassLike.class, 10, true, false, Found -> ControlBodies.isPersonLike(Found) && Scp016Spawn.isQuarry(Found)));
        }
    }

    @SubscribeEvent
    public static void unmasked(LivingEquipmentChangeEvent Event) {
        if (Event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && Event.getEntity() instanceof DClass Subject && !Subject.level().isClientSide) {
            LabContamination.unprotected(Subject);
        }
    }

    @SubscribeEvent
    public static void fileSubject(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!(Event.getTarget() instanceof DClass Subject) || !Subject.isAlive() || !Subject.keepsRecords() || !(Stack.getItem() instanceof PersonnelFileItem File) || !File.isBlank()) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.sidedSuccess(Event.getLevel().isClientSide));
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        int Number = Subject.assignNumber();
        ItemStack Filed = new ItemStack(DClassRegistry.PersonnelFile.get());
        Filed.set(DClassRegistry.RecordComponent.get(), PersonnelRecord.of(Subject));
        give(User, Event.getHand() == InteractionHand.MAIN_HAND, Stack, Filed);
        ServerLevel World = User.serverLevel();
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : DClassRegistry.FileStampLayers) {
            World.playSound(null, Subject.getX(), Subject.getY() + 1.0, Subject.getZ(), Layer.get(), SoundSource.PLAYERS, 0.8f, Pitch);
        }
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Subject.getX(), Subject.getY() + 2.1, Subject.getZ(), 4, 0.2, 0.1, 0.2, 0.0);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.dclass.filed", DClassNumbers.format(Number), Subject.trait().getName(), Subject.crime().getName())
                .withStyle(ChatFormatting.GOLD), true);
    }

    private static void give(Player User, boolean MainHand, ItemStack Blank, ItemStack Filed) {
        InteractionHand Hand = MainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (User.getAbilities().instabuild) {
            if (!User.getInventory().add(Filed)) {
                User.drop(Filed, false);
            }
            return;
        }
        if (Blank.getCount() == 1) {
            User.setItemInHand(Hand, Filed);
            return;
        }
        Blank.shrink(1);
        if (!User.getInventory().add(Filed)) {
            User.drop(Filed, false);
        }
    }
}
