package net.rai.scp_unleashed.scp3008.furniture.living;

import net.minecraft.util.StringRepresentable;

public enum Scp3008FurnitureAPart implements StringRepresentable {
    Main("main"),
    Extra("extra");

    private final String Name;

    Scp3008FurnitureAPart(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public Scp3008FurnitureAPart other() {
        return this == Main ? Extra : Main;
    }
}
