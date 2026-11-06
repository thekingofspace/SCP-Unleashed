package net.rai.scp_unleashed.scp006;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.plumbing.EntityWashedEvent;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp006Events {
    private Scp006Events() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerItem(Capabilities.FluidHandler.ITEM, (Stack, Context) -> new Scp006VesselHandler(Stack), Scp006Registry.Vessel.get());
        Event.registerItem(Capabilities.FluidHandler.ITEM, (Stack, Context) -> new FluidBucketWrapper(Stack), Scp006Registry.Bucket.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp006Registry.Vessel.get());
            Event.accept(Scp006Registry.Bucket.get());
        }
    }

    @SubscribeEvent
    public static void washed(EntityWashedEvent Event) {
        if (Event.getEntity() instanceof LivingEntity Living && Event.getLiquid().getFluid().isSame(Scp006Registry.Water.get())) {
            Scp006Mending.touch(Event.getLevel(), Living);
        }
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent Event) {
        Event.enqueueWork(() -> {
            FluidInteractionRegistry.addInteraction(NeoForgeMod.LAVA_TYPE.value(), new FluidInteractionRegistry.InteractionInformation(Scp006Registry.WaterType.get(),
                    State -> State.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState()));
            DispenserBlock.registerBehavior(Scp006Registry.Bucket.get(), new BucketDispense());
        });
    }

    public static final class BucketDispense extends DefaultDispenseItemBehavior {
        private final DefaultDispenseItemBehavior Fallback = new DefaultDispenseItemBehavior();

        @Override
        protected ItemStack execute(BlockSource Source, ItemStack Stack) {
            DispensibleContainerItem Container = (DispensibleContainerItem) Stack.getItem();
            BlockPos Pos = Source.pos().relative(Source.state().getValue(DispenserBlock.FACING));
            Level World = Source.level();
            if (Container.emptyContents(null, World, Pos, null, Stack)) {
                Container.checkExtraContent(null, World, Stack, Pos);
                return consumeWithRemainder(Source, Stack, new ItemStack(Items.BUCKET));
            }
            return Fallback.dispense(Source, Stack);
        }
    }
}
