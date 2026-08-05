package com.iscript.imson.script.api;

import org.graalvm.polyglot.HostAccess;

public class JavaTypeHelper {
    @HostAccess.Export
    public Class<?> type(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className);
        }
    }
}