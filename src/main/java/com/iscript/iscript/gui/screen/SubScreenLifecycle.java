package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SubScreenLifecycle {
    private final DashboardScreen.SubScreen screen;
    private final State state;

    private final SearchManager search = new SearchManager();
    private final SelectionManager selection = new SelectionManager();
    private final SaveManager save = new SaveManager();
    private final CanvasManager canvas = new CanvasManager();
    private final ModalManager modals = new ModalManager();
    private final EditorManager editors = new EditorManager();

    public SubScreenLifecycle(DashboardScreen.SubScreen screen) {
        this.screen = screen;
        this.state = new State();
    }

    public SubScreenLifecycle(DashboardScreen.SubScreen screen, State state) {
        this.screen = screen;
        this.state = state;
    }

    public State state() { return state; }
    public SearchManager search() { return search; }
    public SelectionManager selection() { return selection; }
    public SaveManager save() { return save; }
    public CanvasManager canvas() { return canvas; }
    public ModalManager modals() { return modals; }
    public EditorManager editors() { return editors; }

    public void init() {
        search.createIfRequested();
        modals.restoreOpen();
        editors.restore();
    }

    public void removed() {
        search.captureAndRemove();
        editors.captureAndRemove();
        modals.captureAndRemove();
        canvas.saveCurrent();
    }

    public void tick(Runnable doSave) {
        save.tick(doSave);
        search.recreateIfMissing();
    }

    public static class State {
        public String lastSearch = "";
        public String selectedId = null;
        public int scroll = 0;
        public int editorScroll = 0;
        public int itemPickerScroll = 0;
        public int targetSelectorScroll = 0;
        public int typePickerScroll = 0;
        public int scriptDropdownScroll = 0;
        public int prereqMenuScroll = 0;
        public int objTypeMenuScroll = 0;
        public boolean dirty = false;
        public int saveDebounce = 0;
        public String saveStatus = "";
        public int saveStatusTimer = 0;
        public String pendingSwitchId = null;
        public int editorTab = 0;
        public int expandedStage = -1;
        public int expandedObjective = -1;
        public Map<String, String> modalFieldValues = new HashMap<>();
        public Map<String, Boolean> modalOpenFlags = new HashMap<>();
        public Map<String, String> editorValues = new HashMap<>();
        public Map<String, double[]> savedCanvasPositions = new HashMap<>();
        public double canvasX = 0, canvasY = 0, zoom = 1.0;
        public String lastEditText = null;
        public int savedScrollOffset = 0;
        public int savedHorizontalScrollOffset = 0;
        public int savedCursorPos = 0;
        public int savedSelectStart = -1;
    }

    public class SearchManager {
        private boolean requested = false;
        private int x, y, w, h;
        private Component label;
        private EditBox box;

        public void request(int x, int y, int w, int h, Component label) {
            this.requested = true;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label;
        }

        public EditBox createIfRequested() {
            if (!requested || screen.getMinecraft() == null) return null;
            box = new EditBox(screen.getMinecraft().font, x, y, w, h, label);
            box.setMaxLength(64);
            box.setTextColor(Theme.TEXT);
            box.setValue(state.lastSearch);
            box.setResponder(s -> { state.lastSearch = s; state.scroll = 0; });
            screen.parent.addWidget(box);
            return box;
        }

        public void captureAndRemove() {
            if (box != null) {
                state.lastSearch = box.getValue();
                screen.parent.removeEditorWidget(box);
                box = null;
            }
        }

        public void recreateIfMissing() {
            if (requested && box == null && screen.getMinecraft() != null) {
                createIfRequested();
            }
        }

        public void setPos(int x, int y, int w, int h) {
            if (box != null) {
                box.setX(x); box.setY(y);
                box.setWidth(w); box.setHeight(h);
            }
        }

        public void setVisible(boolean v) { if (box != null) box.setVisible(v); }
        public EditBox box() { return box; }
        public boolean isRequested() { return requested; }
    }

    public class SelectionManager {
        public String get() { return state.selectedId; }
        public void set(String id) { state.selectedId = id; }
        public int scroll() { return state.scroll; }
        public void scroll(int v) { state.scroll = v; }
        public int editorScroll() { return state.editorScroll; }
        public void editorScroll(int v) { state.editorScroll = v; }
    }

    public class SaveManager {
        public void debounce(int ticks) { state.saveDebounce = ticks; state.dirty = true; }
        public void markDirty() { state.dirty = true; }
        public boolean isDirty() { return state.dirty; }
        public void clearDirty() { state.dirty = false; }

        public void status(String msg, int timer) {
            state.saveStatus = msg;
            state.saveStatusTimer = timer;
        }

        public String status() { return state.saveStatus; }
        public int statusTimer() { return state.saveStatusTimer; }

        public void tick(Runnable doSave) {
            if (state.saveDebounce > 0) {
                state.saveDebounce--;
                if (state.saveDebounce == 0 && doSave != null) {
                    doSave.run();
                    state.dirty = false;
                }
            }
            if (state.saveStatusTimer > 0) {
                state.saveStatusTimer--;
                if (state.saveStatusTimer == 0) {
                    if (state.saveStatus.equals(I18n.s("iscript.script.status.saving"))) {
                        state.saveStatus = I18n.s("iscript.script.status.saved");
                        state.saveStatusTimer = 40;
                    } else {
                        state.saveStatus = "";
                    }
                }
            }
        }
    }

    public class CanvasManager {
        public void saveFor(String id) {
            if (id != null) state.savedCanvasPositions.put(id, new double[]{state.canvasX, state.canvasY, state.zoom});
        }

        public void loadFor(String id) {
            double[] pos = state.savedCanvasPositions.get(id);
            if (pos != null) {
                state.canvasX = pos[0];
                state.canvasY = pos[1];
                state.zoom = pos[2];
            } else {
                state.canvasX = 0;
                state.canvasY = 0;
                state.zoom = 1.0;
            }
        }

        public void saveCurrent() {
        }

        public double x() { return state.canvasX; }
        public double y() { return state.canvasY; }
        public double zoom() { return state.zoom; }
        public void x(double v) { state.canvasX = v; }
        public void y(double v) { state.canvasY = v; }
        public void zoom(double v) { state.zoom = v; }
    }

    public class ModalManager {
        private final Map<String, ModalDef> defs = new HashMap<>();

        public static class ModalDef {
            public String id;
            public Supplier<Boolean> isOpen;
            public Consumer<Boolean> setOpen;
            public Runnable onCreate;
            public Runnable onDestroy;
            public List<String> fieldIds = new ArrayList<>();
        }

        public void register(String id, Supplier<Boolean> isOpen, Consumer<Boolean> setOpen, Runnable onCreate, Runnable onDestroy, String... fieldIds) {
            ModalDef d = new ModalDef();
            d.id = id;
            d.isOpen = isOpen;
            d.setOpen = setOpen;
            d.onCreate = onCreate;
            d.onDestroy = onDestroy;
            for (String f : fieldIds) d.fieldIds.add(f);
            defs.put(id, d);
        }

        public void restoreOpen() {
            for (ModalDef d : defs.values()) {
                if (state.modalOpenFlags.getOrDefault(d.id, false)) {
                    if (d.setOpen != null) d.setOpen.accept(true);
                    if (d.onCreate != null) d.onCreate.run();
                }
            }
        }

        public void captureAndRemove() {
            for (ModalDef d : defs.values()) {
                boolean open = d.isOpen != null && Boolean.TRUE.equals(d.isOpen.get());
                state.modalOpenFlags.put(d.id, open);
                if (d.onDestroy != null) d.onDestroy.run();
            }
        }

        public void open(String id) {
            state.modalOpenFlags.put(id, true);
            ModalDef d = defs.get(id);
            if (d != null && d.setOpen != null) d.setOpen.accept(true);
        }

        public void close(String id) {
            state.modalOpenFlags.put(id, false);
            ModalDef d = defs.get(id);
            if (d != null && d.setOpen != null) d.setOpen.accept(false);
        }

        public boolean isOpen(String id) {
            return state.modalOpenFlags.getOrDefault(id, false);
        }
    }

    public class EditorManager {
        private final Map<String, EditBox> boxes = new HashMap<>();
        private final Map<String, MultiLineEditBox> multiBoxes = new HashMap<>();

        public EditBox addBox(String id, int x, int y, int w, int h, Component label, String initial) {
            if (screen.getMinecraft() == null) return null;
            String val = state.editorValues.getOrDefault(id, initial != null ? initial : "");
            EditBox box = new EditBox(screen.getMinecraft().font, x, y, w, h, label);
            box.setValue(val);
            box.setMaxLength(512);
            screen.parent.addWidget(box);
            boxes.put(id, box);
            return box;
        }

        public EditBox addBox(String id, int x, int y, int w, int h, Component label) {
            return addBox(id, x, y, w, h, label, "");
        }

        public EditBox addNumericBox(String id, int x, int y, int w, int h, Component label, String initial) {
            EditBox box = addBox(id, x, y, w, h, label, initial);
            if (box != null) box.setFilter(s -> s.matches("\\d*"));
            return box;
        }

        public MultiLineEditBox addMultiBox(String id, int x, int y, int w, int h, Component label, String initial) {
            if (screen.getMinecraft() == null) return null;
            String val = state.editorValues.getOrDefault(id, initial != null ? initial : "");
            MultiLineEditBox box = new MultiLineEditBox(screen.getMinecraft().font, x, y, w, h, label, Component.empty());
            box.setValue(val);
            screen.parent.addWidget(box);
            multiBoxes.put(id, box);
            return box;
        }

        public void remove(String id) {
            EditBox b = boxes.remove(id);
            if (b != null) screen.parent.removeEditorWidget(b);
            MultiLineEditBox m = multiBoxes.remove(id);
            if (m != null) screen.parent.removeEditorWidget(m);
        }

        public void removeAll() {
            for (EditBox b : boxes.values()) screen.parent.removeEditorWidget(b);
            boxes.clear();
            for (MultiLineEditBox m : multiBoxes.values()) screen.parent.removeEditorWidget(m);
            multiBoxes.clear();
        }

        public void captureAndRemove() {
            for (Map.Entry<String, EditBox> e : boxes.entrySet()) {
                state.editorValues.put(e.getKey(), e.getValue().getValue());
                screen.parent.removeEditorWidget(e.getValue());
            }
            boxes.clear();
            for (Map.Entry<String, MultiLineEditBox> e : multiBoxes.entrySet()) {
                state.editorValues.put(e.getKey(), e.getValue().getValue());
                screen.parent.removeEditorWidget(e.getValue());
            }
            multiBoxes.clear();
        }

        public void restore() {
        }

        public EditBox box(String id) { return boxes.get(id); }
        public MultiLineEditBox multi(String id) { return multiBoxes.get(id); }
        public boolean has(String id) { return boxes.containsKey(id) || multiBoxes.containsKey(id); }
    }
}