package com.iscript.iscript.data.state;

import java.util.ArrayList;
import java.util.List;

public class ClientNodeData {
    public String id;
    public String name;
    public int color;
    public int posX;
    public int posY;
    public List<ClientTransitionData> transitions = new ArrayList<>();
}