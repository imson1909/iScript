package com.iscript.iscript.gui.undo;

public interface Command {
    void execute();
    void undo();
}