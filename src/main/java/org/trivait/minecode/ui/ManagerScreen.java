// src/main/java/org/trivait/minecode/ui/ManagerScreen.java
package org.trivait.minecode.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.trivait.minecode.engine.ScriptManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.trivait.minecode.util.PathsUtil.getScriptsDir;

public class ManagerScreen extends Screen {
    private List<Path> scripts;
    private Path selected;
    private TextFieldWidget search;
    private float anim;

    public ManagerScreen() {
        super(Text.translatable("minecode.screen.manager"));
    }

    @Override
    protected void init() {
        anim = 0f;
        reloadScripts("");

        int w = this.width;
        int h = this.height;

        search = new TextFieldWidget(this.textRenderer, w/2 - 180, 30, 360, 20, Text.literal(""));
        search.setPlaceholder(Text.translatable("minecode.placeholder.search"));
        search.setChangedListener(this::reloadScripts);
        addSelectableChild(search);

        int bw = 90, bh = 20, gap = 10;
        int total = bw*4 + gap*3;
        int bx = w/2 - total/2;
        int by = h - 60;

        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.new"), btn -> {
            this.client.setScreen(new EditorScreen(null));
        }).dimensions(bx, by, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.api"), btn -> {
            this.client.setScreen(new APIScreen(this));
        }).dimensions(bx + (bw+gap)*1, by, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.run"), btn -> {
            ScriptManager.select(selected);
            ScriptManager.runSelected(client);
        }).dimensions(bx + (bw+gap)*2, by, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.stop"), btn -> {
            ScriptManager.stopRunning();
        }).dimensions(bx + (bw+gap)*3, by, bw, bh).build());
    }

    private void reloadScripts(String query) {
        try {
            scripts = Files.list(getScriptsDir())
                    .filter(p -> p.getFileName().toString().endsWith(".mc"))
                    .filter(p -> query == null || query.isEmpty() ||
                            p.getFileName().toString().toLowerCase().contains(query.toLowerCase()))
                    .sorted()
                    .toList();
            if (selected == null && !scripts.isEmpty()) selected = scripts.get(0);
        } catch (IOException e) {
            this.client.inGameHud.setOverlayMessage(Text.literal("Ошибка списка: " + e.getMessage()), false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // super в начале
        anim = Math.min(anim + delta * 0.05f, 1f);

        int w = this.width;
        int h = this.height;
        drawTitle(ctx, w, h);
        search.render(ctx, mouseX, mouseY, delta);

        int listTop = 60;
        int itemHeight = 24;
        int visible = (h - listTop - 80)/itemHeight;

        int slide = (int)((1f - anim) * 20); // лёгкий слайд сверху вниз

        for (int i=0; i<Math.min(visible, scripts.size()); i++) {
            Path p = scripts.get(i);
            int y = listTop + i*itemHeight + slide;
            boolean sel = p.equals(selected);
            int alpha = sel ? 180 : 100;
            int bg = (alpha << 24) | (sel ? 0x66BFFF : 0x333333); // мягкий голубой для выделения
            ctx.fill(w/2 - 200, y, w/2 + 200, y + itemHeight - 2, bg);
            String name = p.getFileName().toString();
            ctx.drawText(this.textRenderer, name, w/2 - 190, y + 6, 0xFFFFFF, false);

            if (isMouseOverItem(mouseX, mouseY, w/2 - 200, y, 400, itemHeight - 2)) {
                String hint = sel ? "Enter: edit / F6: run" : "Click: select";
                ctx.drawText(this.textRenderer, hint, w/2 + 200 - 160, y + 6, 0xCCCCCC, false);
            }
        }
    }

    private void drawTitle(DrawContext ctx, int w, int h) {
        int base = 0x55000000;
        ctx.fill(0, 0, w, 50, base);
        String t = Text.translatable("minecode.title").getString();
        int tw = textRenderer.getWidth(t);
        int x = (int) (w/2 - tw/2);
        int y = 16;
        int col = 0xFFFFFF | ((int)(anim*255) << 24);
        ctx.drawText(textRenderer, t, x, y, col, false);
    }

    private boolean isMouseOverItem(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int w = this.width;
        int listTop = 60;
        int itemHeight = 24;
        for (int i=0; i<scripts.size(); i++) {
            int y = listTop + i*itemHeight;
            if (isMouseOverItem((int)mx, (int)my, w/2 - 200, y, 400, itemHeight - 2)) {
                selected = scripts.get(i);
                if (button == 1) { // ПКМ открывает редактор
                    this.client.setScreen(new EditorScreen(selected));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && selected != null) {
            this.client.setScreen(new EditorScreen(selected));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
