package com.iscript.iscript.client.camera;

public enum PathType {
    LINEAR,
    CATMULL_ROM,
    CUBIC_BEZIER,
    HERMITE,
    BSPLINE,
    CIRCULAR,
    ELLIPTICAL,
    SPIRAL,
    HELIX,
    STEP,
    NONE;

    public static final PathType[] VALUES = values();
}