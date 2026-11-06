package net.rai.scp_unleashed.notice.client;

import net.rai.scp_unleashed.notice.Notice;

public final class NoticeClientHooks {
    private NoticeClientHooks() {
    }

    public static void receive(Notice Shown) {
        NoticeHud.push(Shown);
    }
}
