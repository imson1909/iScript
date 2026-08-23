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
    @HostAccess.Export public double trunc(double x) { return x >= 0 ? Math.floor(x) : Math.ceil(x); }
    @HostAccess.Export public double cbrt(double x) { return Math.cbrt(x); }
    @HostAccess.Export public double log2(double x) { return Math.log(x) / Math.log(2); }
    @HostAccess.Export public double expm1(double x) { return Math.expm1(x); }
    @HostAccess.Export public double asin(double x) { return Math.asin(x); }
    @HostAccess.Export public double acos(double x) { return Math.acos(x); }
    @HostAccess.Export public double atan(double x) { return Math.atan(x); }
    @HostAccess.Export public double atan2(double y, double x) { return Math.atan2(y, x); }
    @HostAccess.Export public double sinh(double x) { return Math.sinh(x); }
    @HostAccess.Export public double cosh(double x) { return Math.cosh(x); }
    @HostAccess.Export public double tanh(double x) { return Math.tanh(x); }
    @HostAccess.Export public double hypot(double a, double b) { return Math.hypot(a, b); }
    @HostAccess.Export public double sign(double x) { return Math.signum(x); }
    @HostAccess.Export public long clz32(int x) { return Integer.numberOfLeadingZeros(x); }
    @HostAccess.Export public long imul(int a, int b) { return (long) a * (long) b; }
    @HostAccess.Export public float fround(double x) { return (float) x; }
    @HostAccess.Export public final double PI = Math.PI;
    @HostAccess.Export public final double E = Math.E;
    @HostAccess.Export public final double SQRT2 = Math.sqrt(2);
    @HostAccess.Export public final double SQRT1_2 = Math.sqrt(0.5);
    @HostAccess.Export public final double LN2 = Math.log(2);
    @HostAccess.Export public final double LN10 = Math.log(10);
    @HostAccess.Export public final double LOG2E = Math.log(Math.E) / Math.log(2);
    @HostAccess.Export public final double LOG10E = Math.log10(Math.E);
}