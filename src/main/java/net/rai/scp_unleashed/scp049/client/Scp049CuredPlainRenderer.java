package net.rai.scp_unleashed.scp049.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.monster.Zombie;

public class Scp049CuredPlainRenderer extends ZombieRenderer {
    public Scp049CuredPlainRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    protected boolean shouldShowName(Zombie Risen) {
        return false;
    }
}
