package net.rai.scp_unleashed.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.TransferBlock;
import net.rai.scp_unleashed.block.entity.TransferBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class TransferClientEvents {
    @SubscribeEvent
    public static void wrapTransferModels(ModelEvent.ModifyBakingResult Event) {
        for (Block Candidate : BuiltInRegistries.BLOCK) {
            if (Candidate instanceof TransferBlock) {
                for (BlockState State : Candidate.getStateDefinition().getPossibleStates()) {
                    Event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(State), (Location, Original) -> new TransferBlockModel(Original));
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block Event) {
        Block[] Transfers = BuiltInRegistries.BLOCK.stream().filter(Candidate -> Candidate instanceof TransferBlock).toArray(Block[]::new);
        Event.register((State, World, Pos, TintIndex) -> World != null && Pos != null && World.getBlockEntity(Pos) instanceof TransferBlockEntity Transfer && Transfer.hasDisguise()
                ? Event.getBlockColors().getColor(Transfer.getDisguise(), World, Pos, TintIndex) : -1, Transfers);
    }
}
