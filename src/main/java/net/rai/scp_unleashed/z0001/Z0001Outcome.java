package net.rai.scp_unleashed.z0001;

public enum Z0001Outcome {
    Taken("spent"),
    NoTake("no_take"),
    Faded("fades"),
    Spent("spent"),
    Invalid("no_take");

    private final String HintKey;

    Z0001Outcome(String HintKey) {
        this.HintKey = HintKey;
    }

    public String hintKey() {
        return "gui.scp_unleashed.z0001_book." + HintKey;
    }

    public boolean keepsInk() {
        return this == Taken;
    }

    public static Z0001Outcome byId(int Id) {
        Z0001Outcome[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
