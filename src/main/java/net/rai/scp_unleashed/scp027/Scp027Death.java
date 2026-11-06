package net.rai.scp_unleashed.scp027;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;

public final class Scp027Death {
    public static final String Prefix = "notice.scp_unleashed.scp027.";
    public static final double NoticeRange = 64.0;

    private Scp027Death() {
    }

    public static int end(Scp027Entity Subject) {
        if (!(Subject.level() instanceof ServerLevel World)) {
            return 0;
        }
        int Scattered = Scp027Swarm.scatterAll(World, Subject);
        Subject.setSwarmSize(0);
        Notices.sendNear(World, Subject.position(), NoticeRange, putDown());
        return Scattered;
    }

    public static Notice putDown() {
        return Notice.anomaly(DocumentClass.Keter, Component.translatable(Prefix + "put_down")).withDetail(Component.translatable(Prefix + "put_down.detail"));
    }
}
