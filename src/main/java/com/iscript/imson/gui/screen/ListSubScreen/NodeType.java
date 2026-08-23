package com.iscript.imson.gui.screen.ListSubScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeType {
    public final String id;
    public final String titleKey;
    public final int headerColor;
    public final Map<String, String> defaultParams;
    public final int outputSlots;
    public final List<String> outputLabels;
    public final List<EditorField> editorFields;

    public NodeType(String id, String titleKey, int headerColor, Map<String, String> defaultParams, int outputSlots, List<String> outputLabels, List<EditorField> editorFields) {
        this.id = id;
        this.titleKey = titleKey;
        this.headerColor = headerColor;
        this.defaultParams = defaultParams != null ? new HashMap<>(defaultParams) : new HashMap<>();
        this.outputSlots = outputSlots;
        this.outputLabels = outputLabels != null ? new ArrayList<>(outputLabels) : new ArrayList<>();
        this.editorFields = editorFields != null ? new ArrayList<>(editorFields) : new ArrayList<>();
    }

    public static class EditorField {
        public final String key;
        public final String labelKey;
        public final FieldType type;

        public EditorField(String key, String labelKey, FieldType type) {
            this.key = key;
            this.labelKey = labelKey;
            this.type = type;
        }
    }

    public enum FieldType {
        STRING, MULTILINE, NUMBER
    }

    public static Builder builder(String id, String titleKey) {
        return new Builder(id, titleKey);
    }

    public static class Builder {
        private final String id;
        private final String titleKey;
        private int headerColor = 0xFF444444;
        private Map<String, String> defaultParams = new HashMap<>();
        private int outputSlots = 1;
        private List<String> outputLabels = new ArrayList<>();
        private List<EditorField> editorFields = new ArrayList<>();

        private Builder(String id, String titleKey) {
            this.id = id;
            this.titleKey = titleKey;
        }

        public Builder color(int c) {
            this.headerColor = c;
            return this;
        }

        public Builder param(String key, String value) {
            this.defaultParams.put(key, value);
            return this;
        }

        public Builder outputs(int n) {
            this.outputSlots = n;
            return this;
        }

        public Builder outputLabels(String... labels) {
            Collections.addAll(this.outputLabels, labels);
            return this;
        }

        public Builder field(String key, String labelKey, FieldType type) {
            this.editorFields.add(new EditorField(key, labelKey, type));
            return this;
        }

        public NodeType build() {
            return new NodeType(id, titleKey, headerColor, defaultParams, outputSlots, outputLabels, editorFields);
        }
    }
}