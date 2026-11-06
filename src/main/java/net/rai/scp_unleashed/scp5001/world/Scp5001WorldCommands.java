package net.rai.scp_unleashed.scp5001.world;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp5001WorldCommands {
    private Scp5001WorldCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp5001")
                        .then(Commands.literal("site").requires(Source -> Source.hasPermission(2)).executes(Context -> site(Context.getSource()))
                                .then(Commands.literal("reset").executes(Context -> reset(Context.getSource()))))));
    }

    private static int site(CommandSourceStack Source) {
        Scp5001Sites Data = Scp5001Sites.get(Source.getServer());
        if (!Data.planned()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.none"), false);
            return 0;
        }
        BoundingBox Box = Data.bounds();
        BlockPos Door = Data.door();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.door", Door.getX(), Door.getY(), Door.getZ()), false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.bounds", Box.minX(), Box.minY(), Box.minZ(), Box.maxX(), Box.maxY(), Box.maxZ()),
                false);
        if (Data.legacy()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.legacy"), false);
            return 1;
        }
        int Rooms = Scp5001Emitter.plan(Data.seed()).roomCount();
        int Total = Data.chunks().size();
        int Built = Data.finished();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.progress", Rooms, Built, Total, Scp5001Emitter.pending()), false);
        return 1;
    }

    private static int reset(CommandSourceStack Source) {
        Scp5001Sites.get(Source.getServer()).forget();
        Scp5001Emitter.clear();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp5001_world.site.reset"), true);
        return 1;
    }
}
