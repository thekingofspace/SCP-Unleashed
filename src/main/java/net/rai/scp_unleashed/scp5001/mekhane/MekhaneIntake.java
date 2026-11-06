package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;

public final class MekhaneIntake {
    private MekhaneIntake() {
    }

    public static boolean open(MinecraftServer Server) {
        return MekhaneProtocol.get(Server).stage() == MekhaneProtocol.Stage.Engaged && MekhaneDirector.awake(Scp5001State.get(Server).phase());
    }

    public static int accept(MinecraftServer Server, ItemStack Stack, boolean Simulate) {
        MekhaneValues.Worth Worth = MekhaneValues.worth(Stack);
        if (Worth == null || !open(Server)) {
            return 0;
        }
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        long Remaining = Protocol.need(Worth.Sort()) - Protocol.amount(Worth.Sort());
        if (Remaining <= 0) {
            return 0;
        }
        long Wanted = (Remaining + Worth.Units() - 1) / Worth.Units();
        int Taken = (int) Math.min(Stack.getCount(), Wanted);
        if (!Simulate && Taken > 0) {
            Protocol.add(Worth.Sort(), (long) Taken * Worth.Units());
        }
        return Taken;
    }
}
