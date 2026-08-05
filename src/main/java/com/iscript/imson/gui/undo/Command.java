package com.iscript.imson.gui.undo;

public interface Command {
    void execute();
    void undo();
}