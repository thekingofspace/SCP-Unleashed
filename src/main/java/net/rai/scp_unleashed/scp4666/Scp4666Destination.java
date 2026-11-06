package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;

public final class Scp4666Destination {
    public static final ResourceLocation Id = Scp4666Registry.location("scp4666_winter");
    public static final ResourceLocation StandinId = Scp4666Registry.location("scp4666_winter_standin");
    public static final int Tint = 0xA8DCFF;
    public static final int ShardVariant = 1;
    public static final int WarningLines = 3;

    private Scp4666Destination() {
    }

    public static GatewayDestination make() {
        return make(Id, Scp4666Registry.Dimension);
    }

    public static GatewayDestination make(ResourceLocation Name, ResourceKey<Level> Dimension) {
        return new GatewayDestination(Name, Dimension, Tint, ShardVariant, WarningLines, Scp4666Destination::entry, GatewayDestination.SealedTravel, new Winter());
    }

    public static GatewayDestination standIn(ResourceKey<Level> Dimension) {
        GatewayDestination Found = GatewayDestinations.get(StandinId);
        if (Found == null) {
            Found = GatewayDestinations.register(make(StandinId, Dimension));
        }
        Scp4666World.standIn(Found.Dimension());
        return Found;
    }

    private static GatewayDestination.Arrival entry(ServerLevel Target, ServerPlayer User) {
        int Index = Target.players().size();
        return new GatewayDestination.Arrival(Scp4666World.arrival(Target.getServer(), Index), Scp4666Map.get(Target.getServer()).SpawnYaw);
    }

    private static final class Winter implements GatewayDestination.Hooks {
        @Override
        public boolean canEnter(ServerPlayer User, ServerLevel Target) {
            int Phase = Scp4666State.get(User.server).phase();
            if (Phase == Scp4666State.PhaseEnding) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.sealed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return false;
            }
            return true;
        }

        @Override
        public boolean binds(ServerPlayer User) {
            return Scp4666World.participant(User);
        }

        @Override
        public void prepare(ServerLevel Target) {
            Scp4666World.ensureBuilt(Target);
        }

        @Override
        public void arrived(ServerPlayer User, ServerLevel Target) {
            Scp4666Director.joined(User);
        }

        @Override
        public void left(ServerPlayer User, ServerLevel From) {
            Scp4666Director.left(User);
        }

        @Override
        public void died(ServerPlayer User, ServerLevel Where) {
            Scp4666Director.died(User);
        }
    }
}
