package net.rai.scp_unleashed.notice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.notice.NoticeIcon;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoticeIcons {
    public static final String Folder = "textures/gui/notice/";
    public static final ResourceLocation UnknownClass = location("class/unknown");
    public static final ResourceLocation FoundationLogo = location("stranger/foundation");
    public static final ResourceLocation InfoIcon = location("generic/info");
    private static final Map<NoticeIcon, ResourceLocation> Resolved = new ConcurrentHashMap<>();

    private NoticeIcons() {
    }

    public static ResourceLocation texture(NoticeIcon Icon) {
        return Resolved.computeIfAbsent(Icon, NoticeIcons::resolve);
    }

    public static ResourceLocation classIcon(String ClassName) {
        return pick(location("class/" + ClassName), UnknownClass);
    }

    public static void clear() {
        Resolved.clear();
    }

    private static ResourceLocation resolve(NoticeIcon Icon) {
        return switch (Icon.Type()) {
            case Anomaly -> classIcon(Icon.Id());
            case Stranger -> pick(location("stranger/" + Icon.Id()), FoundationLogo);
            case Generic -> pick(location("generic/" + Icon.Id()), InfoIcon);
            case Texture -> pick(ResourceLocation.tryParse(Icon.Id()), InfoIcon);
        };
    }

    private static ResourceLocation pick(@Nullable ResourceLocation Wanted, ResourceLocation Fallback) {
        if (Wanted == null) {
            return Fallback;
        }
        return Minecraft.getInstance().getResourceManager().getResource(Wanted).isPresent() ? Wanted : Fallback;
    }

    @Nullable
    private static ResourceLocation location(String Name) {
        return ResourceLocation.tryBuild(Scp_unleashed.ModId, Folder + Name + ".png");
    }
}
