package net.rai.scp_unleashed.pressure;

public enum PortRole {
    Input,
    Output,
    Both;

    public boolean canFill() {
        return this != Output;
    }

    public boolean canDrain() {
        return this != Input;
    }
}
