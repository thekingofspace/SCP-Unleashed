package net.rai.scp_unleashed.scp354;

public interface Scp354Swimmer {
    boolean swimsOut();

    double swimSpeed();

    double draft();

    double haulReach();

    int haulTicks();

    Scp354Swim.Course course();

    void beginHaul();

    boolean isHauling();
}
