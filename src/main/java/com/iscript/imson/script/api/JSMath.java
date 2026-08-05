package com.iscript.imson.script.api;

import org.graalvm.polyglot.HostAccess;

public class JSMath {
    @HostAccess.Export public double random() { return Math.random(); }
    @HostAccess.Export public double floor(double x) { return Math.floor(x); }
    @HostAccess.Export public double ceil(double x) { return Math.ceil(x); }
    @HostAccess.Export public long round(double x) { return Math.round(x); }
    @HostAccess.Export public double max(double a, double b) { return Math.max(a, b); }
    @HostAccess.Export public double min(double a, double b) { return Math.min(a, b); }
    @HostAccess.Export public double sin(double x) { return Math.sin(x); }
    @HostAccess.Export public double cos(double x) { return Math.cos(x); }
    @HostAccess.Export public double tan(double x) { return Math.tan(x); }
    @HostAccess.Export public double sqrt(double x) { return Math.sqrt(x); }
    @HostAccess.Export public double pow(double a, double b) { return Math.pow(a, b); }
    @HostAccess.Export public double abs(double x) { return Math.abs(x); }
    @HostAccess.Export public double log(double x) { return Math.log(x); }
    @HostAccess.Export public double log10(double x) { return Math.log10(x); }
    @HostAccess.Export public double exp(double x) { return Math.exp(x); }
    @HostAccess.Export public double toRadians(double x) { return Math.toRadians(x); }
    @HostAccess.Export public double toDegrees(double x) { return Math.toDegrees(x); }
    @HostAccess.Export public final double PI = Math.PI;
    @HostAccess.Export public final double E = Math.E;
}