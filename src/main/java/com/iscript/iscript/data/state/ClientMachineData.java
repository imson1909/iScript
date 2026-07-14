package com.iscript.iscript.data.state;

import java.util.ArrayList;
import java.util.List;

public class ClientMachineData {
    public String id;
    public String name;
    public String entryNode;
    public List<ClientNodeData> nodes = new ArrayList<>();
}