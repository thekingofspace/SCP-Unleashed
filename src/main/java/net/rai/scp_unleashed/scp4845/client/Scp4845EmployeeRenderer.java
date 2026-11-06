package net.rai.scp_unleashed.scp4845.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;

public class Scp4845EmployeeRenderer extends DClassRenderer<Scp4845Employee> {
    public Scp4845EmployeeRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp4845EmployeeModel());
    }
}
