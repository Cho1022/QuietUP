package com.quietup.noise.entity;

public enum NoiseDirection {
    UP(1, "위쪽", "위층 이웃"),
    DOWN(-1, "아래쪽", "아래층 이웃");

    private final int floorOffset;
    private final String directionLabel;
    private final String targetLabel;

    NoiseDirection(int floorOffset, String directionLabel, String targetLabel) {
        this.floorOffset = floorOffset;
        this.directionLabel = directionLabel;
        this.targetLabel = targetLabel;
    }

    public int targetFloor(int currentFloor) {
        return currentFloor + floorOffset;
    }

    public String directionLabel() {
        return directionLabel;
    }

    public String targetLabel() {
        return targetLabel;
    }
}
