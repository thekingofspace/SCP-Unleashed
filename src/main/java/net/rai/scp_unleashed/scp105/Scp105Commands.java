package net.rai.scp_unleashed.scp105;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp105Commands {
    public static final String Prefix = "commands.scp_unleashed.scp105.";
    public static final double GiftSearch = 16.0;

    private Scp105Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        CommandBuildContext Context = Event.getBuildContext();
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp105")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Run -> spawn(Run.getSource(), Run.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Run -> spawn(Run.getSource(), Vec3Argument.getVec3(Run, "pos")))))
                        .then(Commands.literal("photo")
                                .executes(Run -> randomPhoto(Run.getSource()))
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item", ItemArgument.item(Context))
                                                .executes(Run -> itemPhoto(Run.getSource(), ItemArgument.getItem(Run, "item").getItem(), 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(Run -> itemPhoto(Run.getSource(), ItemArgument.getItem(Run, "item").getItem(), IntegerArgumentType.getInteger(Run, "count"))))))
                                .then(Commands.literal("creature")
                                        .then(Commands.argument("entity", ResourceArgument.resource(Context, Registries.ENTITY_TYPE))
                                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                .executes(Run -> creaturePhoto(Run.getSource(), ResourceArgument.getSummonableEntityType(Run, "entity").value(), false))
                                                .then(Commands.argument("young", BoolArgumentType.bool())
                                                        .executes(Run -> creaturePhoto(Run.getSource(), ResourceArgument.getSummonableEntityType(Run, "entity").value(),
                                                                BoolArgumentType.getBool(Run, "young")))))))
                        .then(Commands.literal("gift")
                                .executes(Run -> gift(Run.getSource())))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Optional<Scp105Iris> Placed = Scp105Arrival.place(Source.getLevel(), Spot, Yaw);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int hand(CommandSourceStack Source, Scp105Photo Photo) throws CommandSyntaxException {
        ServerPlayer Target = Source.getPlayerOrException();
        ItemStack Stack = Scp105PhotographItem.create(Photo);
        if (!Target.getInventory().add(Stack)) {
            Target.drop(Stack, false);
        }
        Component Name = Scp105Subjects.displayName(Photo);
        Source.sendSuccess(() -> Component.translatable(Prefix + "photo.success", Name, Photo.Count()), true);
        return 1;
    }

    private static int randomPhoto(CommandSourceStack Source) throws CommandSyntaxException {
        return hand(Source, Scp105Subjects.roll(Source.getLevel().getRandom(), Scp105PhotographItem.peaceful(Source.getLevel())));
    }

    private static int itemPhoto(CommandSourceStack Source, Item Thing, int Count) throws CommandSyntaxException {
        if (!Scp105Subjects.allowedItem(Thing)) {
            Source.sendFailure(Component.translatable(Prefix + "photo.blocked", new ItemStack(Thing).getHoverName()));
            return 0;
        }
        return hand(Source, Scp105Subjects.made(Scp105Photo.Kind.Thing, BuiltInRegistries.ITEM.getKey(Thing), Count, false, Source.getLevel().getRandom()));
    }

    private static int creaturePhoto(CommandSourceStack Source, EntityType<?> Type, boolean Young) throws CommandSyntaxException {
        if (!Scp105Subjects.allowedCreature(Type) || !Scp105Subjects.livingCreature(Type, Source.getLevel())) {
            Source.sendFailure(Component.translatable(Prefix + "photo.blocked", Type.getDescription()));
            return 0;
        }
        boolean Grows = Young && Scp105Subjects.grows(Type, Source.getLevel());
        return hand(Source, Scp105Subjects.made(Scp105Photo.Kind.Creature, BuiltInRegistries.ENTITY_TYPE.getKey(Type), 1, Grows, Source.getLevel().getRandom()));
    }

    private static int gift(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer Target = Source.getPlayerOrException();
        List<Scp105Iris> Near = Target.serverLevel().getEntitiesOfClass(Scp105Iris.class, Target.getBoundingBox().inflate(GiftSearch), Found -> Found.isAlive());
        Optional<Scp105Iris> Closest = Near.stream().min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Target)));
        if (Closest.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "gift.none", (int) GiftSearch));
            return 0;
        }
        Scp105Iris Iris = Closest.get();
        if (Iris.act() != Scp105Iris.ActNone) {
            Iris.stopAct();
        }
        Iris.beginGift(Target);
        Iris.say(Target, Component.translatable(Scp105Iris.Prefix + "gift.0"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "gift.success"), true);
        return 1;
    }
}
