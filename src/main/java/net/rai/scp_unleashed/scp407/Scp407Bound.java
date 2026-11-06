package net.rai.scp_unleashed.scp407;

public interface Scp407Bound {
    int Unbound = -1;

    int boundSession();

    void bindSession(int Id);

    void beginWither();

    boolean isWithering();
}
