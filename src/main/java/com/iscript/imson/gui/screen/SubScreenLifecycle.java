package com.iscript.imson.gui.screen;

import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.widget.MultiLineEditBox;
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
    private final FormDialogManager forms = new FormDialogManager();

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
    public FormDialogManager forms() { return forms; }

    public void init() {
        search.createIfRequested();
        modals.restoreOpen();
        editors.restore();
    }

    public void removed() {
        search.captureAndRemove();
        editors.captureAndRemove();
        modals.captureAndRemove();
        forms.closeAll();
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
                if (d.isOpen != null && Boolean.TRUE.equals(d.isOpen.get())) {
                    if (d.setOpen != null) d.setOpen.accept(false);
                    if (d.onDestroy != null) d.onDestroy.run();
                }
                state.modalOpenFlags.put(d.id, false);
            }
        }

        public void open(String id) {
            ModalDef d = defs.get(id);
            if (d == null) return;
            boolean already = state.modalOpenFlags.getOrDefault(id, false);
            state.modalOpenFlags.put(id, true);
            if (d.setOpen != null) d.setOpen.accept(true);
            if (!already && d.onCreate != null) d.onCreate.run();
        }

        public void close(String id) {
            ModalDef d = defs.get(id);
            if (d == null) return;
            boolean wasOpen = state.modalOpenFlags.getOrDefault(id, false);
            state.modalOpenFlags.put(id, false);
            if (d.setOpen != null) d.setOpen.accept(false);
            if (wasOpen && d.onDestroy != null) d.onDestroy.run();
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

    public class FormDialogManager {
        public static class Field {
            public String id;
            public Component label;
            public String initial = "";
            public int maxLength = 64;
            public boolean numeric = false;
            public int width = 200;
            public int height = 20;
        }

        public static class Form {
            public String id;
            public Component title;
            public List<Field> fields = new ArrayList<>();
            public Consumer<Map<String, String>> onConfirm;
            public Runnable onCancel;
        }

        private final Map<String, Form> forms = new HashMap<>();
        private String openFormId = null;
        private final Map<String, EditBox> boxes = new HashMap<>();

        public void register(Form form) { forms.put(form.id, form); }

        public void open(String id) {
            closeAll();
            openFormId = id;
            Form f = forms.get(id);
            if (f == null) return;
            int cx = screen.parent.width / 2;
            int cy = screen.parent.height / 2 - 40;
            int fy = cy + 24;
            for (Field field : f.fields) {
                EditBox box = new EditBox(screen.getMinecraft().font, cx - field.width / 2, fy, field.width, field.height, field.label);
                box.setValue(field.initial);
                box.setMaxLength(field.maxLength);
                if (field.numeric) box.setFilter(s -> s.matches("-?\\d*"));
                screen.parent.addWidget(box);
                boxes.put(field.id, box);
                fy += 26;
            }
        }

        public void close(String id) {
            if (!id.equals(openFormId)) return;
            Form f = forms.get(id);
            if (f != null) {
                for (Field field : f.fields) {
                    EditBox box = boxes.remove(field.id);
                    if (box != null) screen.parent.removeEditorWidget(box);
                }
            }
            openFormId = null;
        }

        public void closeAll() { if (openFormId != null) close(openFormId); }

        public boolean isOpen() { return openFormId != null; }
        public String openId() { return openFormId; }

        public void render(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.gui.Font font, int mx, int my) {
            if (openFormId == null) return;
            Form f = forms.get(openFormId);
            if (f == null) return;
            int cx = screen.parent.width / 2;
            int cy = screen.parent.height / 2 - 40;
            int fh = 60 + f.fields.size() * 26;
            int fx = cx - 110;
            int fy = cy;
            g.fill(fx - 4, fy - 4, fx + 224, fy + fh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            g.fill(fx, fy, fx + 220, fy + fh, Theme.BG_INNER);
            g.renderOutline(fx, fy, 220, fh, Theme.ACCENT);
            g.drawCenteredString(font, f.title.getString(), cx, fy + 6, Theme.ACCENT);
            int by = fy + 28;
            for (Field field : f.fields) {
                EditBox box = boxes.get(field.id);
                if (box != null) { box.setX(cx - field.width / 2); box.setY(by); }
                by += 26;
            }
            boolean okH = mx >= cx - 50 && mx <= cx - 2 && my >= by + 4 && my <= by + 26;
            g.fill(cx - 50, by + 4, cx - 2, by + 26, okH ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(cx - 50, by + 4, 48, 22, Theme.BORDER);
            g.drawCenteredString(font, I18n.s("iscript.button.confirm"), cx - 26, by + 9, okH ? Theme.ACCENT : 0xFF44AA44);
            boolean cancelH = mx >= cx + 2 && mx <= cx + 50 && my >= by + 4 && my <= by + 26;
            g.fill(cx + 2, by + 4, cx + 50, by + 26, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(cx + 2, by + 4, 48, 22, Theme.BORDER);
            g.drawCenteredString(font, I18n.s("iscript.button.cancel"), cx + 26, by + 9, cancelH ? Theme.ERROR : 0xFFAA4444);
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (openFormId == null) return false;
            Form f = forms.get(openFormId);
            if (f == null) return false;
            int cx = screen.parent.width / 2;
            int cy = screen.parent.height / 2 - 40;
            int by = cy + 28 + f.fields.size() * 26;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= by + 4 && my <= by + 26) {
                Map<String, String> values = new HashMap<>();
                for (Field field : f.fields) {
                    EditBox box = boxes.get(field.id);
                    values.put(field.id, box != null ? box.getValue().trim() : "");
                }
                close(openFormId);
                if (f.onConfirm != null) f.onConfirm.accept(values);
                return true;
            }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= by + 4 && my <= by + 26) {
                close(openFormId);
                if (f.onCancel != null) f.onCancel.run();
                return true;
            }
            for (Field field : f.fields) {
                EditBox box = boxes.get(field.id);
                if (box != null && mx >= box.getX() && mx <= box.getX() + box.getWidth() && my >= box.getY() && my <= box.getY() + box.getHeight()) {
                    screen.parent.setFocusedWidget(box);
                    return box.mouseClicked(mx, my, button);
                }
            }
            return true;
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (openFormId == null) return false;
            if (keyCode == 257 || keyCode == 335) {
                Form f = forms.get(openFormId);
                Map<String, String> values = new HashMap<>();
                for (Field field : f.fields) {
                    EditBox box = boxes.get(field.id);
                    values.put(field.id, box != null ? box.getValue().trim() : "");
                }
                close(openFormId);
                if (f.onConfirm != null) f.onConfirm.accept(values);
                return true;
            }
            if (keyCode == 256) {
                Form f = forms.get(openFormId);
                close(openFormId);
                if (f.onCancel != null) f.onCancel.run();
                return true;
            }
            for (EditBox box : boxes.values()) {
                if (box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        public boolean charTyped(char codePoint, int modifiers) {
            if (openFormId == null) return false;
            for (EditBox box : boxes.values()) {
                if (box.isFocused()) return box.charTyped(codePoint, modifiers);
            }
            return true;
        }
    }
}