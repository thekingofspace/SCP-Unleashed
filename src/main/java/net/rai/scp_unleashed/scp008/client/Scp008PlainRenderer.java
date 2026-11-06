package net.rai.scp_unleashed.scp008.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.monster.Zombie;
import net.rai.scp_unleashed.scp008.Scp008Zombie;

public class Scp008PlainRenderer extends ZombieRenderer {
    public Scp008PlainRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    protected boolean isShaking(Zombie Risen) {
        return super.isShaking(Risen) || Risen instanceof Scp008Zombie Infected && Infected.isCollapsing();
    }
}
