package net.rai.scp_unleashed.scp009.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Containment;
import net.rai.scp_unleashed.scp009.Scp009CrystalItem;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.LayoutMotion;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp009Display {
    public static final ModelResourceLocation Cradle = standalone("cradle");
    public static final ModelResourceLocation CradleFrost = standalone("cradle_frost");
    public static final float CradleX = 8.0f;
    public static final float CradleZ = 9.0f;
    public static final float SocketTop = 1.5f;
    public static final float ModelHalf = 8.0f;
    public static final float SmallScale = 0.42f;
    public static final float TallScale = 0.78f;
    public static final float FragmentBoost = 1.4f;
    public static final float RestYaw = -20.0f;
    public static final int PulsePeriod = 10;
    public static final int AlarmTint = 0xFFFFC4C4;
    public static final int AlarmTintDim = 0xFFB89090;
    public static final float TwitchAngle = 5.0f;
    public static final int TwitchEvery = 37;
    private static final LayoutMotion Twitch = LayoutMotion.twitch(TwitchAngle, TwitchEvery);

    private Scp009Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp009/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Cradle, CradleFrost);
    }

    public static void register() {
        StorageLayouts.register(Scp009CrystalItem::is, Scp009Display::render);
    }

    public static void render(StorageLayoutContext Context) {
        boolean Contained = Scp009Containment.isContained(Context.Locker());
        StorageLayoutContext Scoped = Context;
        if (!Contained) {
            boolean Bright = Math.floorMod((int) (Context.time() / PulsePeriod), 2) == 0;
            Scoped = Context.withTint(Bright ? AlarmTint : AlarmTintDim).withLight(InteriorLayout.FullBright);
        }
        Scoped.platform();
        float Top = InteriorLayout.platformTop(Scoped.Tall());
        Scoped.model(Contained ? CradleFrost : Cradle, Placement.at(CradleX, Top, CradleZ));
        if (Scoped.isEmpty()) {
            return;
        }
        ItemStack Shown = Scoped.Stack();
        float Scale = (Scoped.Tall() ? TallScale : SmallScale) * (Shown.is(Scp009Registry.Fragment.get()) ? FragmentBoost : 1.0f);
        Placement Rest = Placement.at(CradleX, Top + SocketTop + ModelHalf * Scale, CradleZ).withYaw(RestYaw).withScale(Scale);
        if (!Contained) {
            float Time = Scoped.time();
            long Seed = Scoped.seed();
            Rest = Rest.withRotation(RestYaw + Twitch.twitchAt(Time, Seed), 0.0f, Twitch.twitchAt(Time + 11.0f, Seed));
        }
        Scoped.item(Shown, Rest, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
