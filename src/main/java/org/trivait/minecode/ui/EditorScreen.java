// src/main/java/org/trivait/minecode/ui/EditorScreen.java
package org.trivait.minecode.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.trivait.minecode.engine.ScriptManager;
import org.trivait.minecode.ui.widget.CodeTextArea;
import org.trivait.minecode.util.PathsUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class EditorScreen extends Screen {
    private final Path scriptPathOrNull;
    private TextFieldWidget nameField;
    private CodeTextArea code;
    private float anim;

    public EditorScreen(Path scriptPathOrNull) {
        super(Text.translatable("minecode.screen.editor"));
        this.scriptPathOrNull = scriptPathOrNull;
    }

    @Override
    protected void init() {
        anim = 0f;
        int w = width;
        int h = height;

        nameField = new TextFieldWidget(textRenderer, w/2 - 200, 30, 200, 20, Text.literal(""));
        nameField.setPlaceholder(Text.translatable("minecode.placeholder.scriptname"));
        if (scriptPathOrNull != null) {
            String base = scriptPathOrNull.getFileName().toString();
            if (base.endsWith(".mc")) base = base.substring(0, base.length()-3);
            nameField.setText(base);
        }
        addSelectableChild(nameField);

        code = new CodeTextArea(textRenderer, w/2 - 200, 60, 400, h - 130);
        code.setHintsProvider(ScriptManager::hintFor);
        if (scriptPathOrNull != null) {
            try {
                String content = Files.readString(scriptPathOrNull);
                code.setText(content);
            } catch (IOException e) {
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("Ошибка чтения: " + e.getMessage()), false);
            }
        } else {
            code.setText("""
                # MineCode example
                say "Hello, World!"
                wait 20
                say "Bye, World!"
                """);
        }
        addSelectableChild(code);

        // Центрируем кнопки
        int bw = 90, bh = 20, gap = 10;
        int total = bw*5 + gap*4;
        int bx = w/2 - total/2;
        int by = h - 60;

        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.save"), btn -> save())
                .dimensions(bx, by, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.run"), btn -> {
            save();
            ScriptManager.select(PathsUtil.resolveScript(nameField.getText()));
            ScriptManager.runSelected(MinecraftClient.getInstance());
        }).dimensions(bx + (bw+gap)*1, by, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.stop"), btn -> {
            ScriptManager.stopRunning();
        }).dimensions(bx + (bw+gap)*2, by, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.api"), btn -> {
            this.client.setScreen(new APIScreen(this));
        }).dimensions(bx + (bw+gap)*3, by, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minecode.button.back"), btn -> {
            this.client.setScreen(new ManagerScreen());
        }).dimensions(bx + (bw+gap)*4, by, bw, bh).build());
    }

    private void save() {
        String nm = nameField.getText().trim();
        if (nm.isEmpty()) {
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("Имя пустое"), false);
            return;
        }
        Path p = PathsUtil.resolveScript(nm);
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, code.getText(), StandardCharsets.UTF_8);
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.translatable("minecode.toast.saved"), false);
        } catch (IOException e) {
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("Ошибка сохранения: " + e.getMessage()), false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_R) {
            save();
            ScriptManager.select(PathsUtil.resolveScript(nameField.getText()));
            ScriptManager.runSelected(MinecraftClient.getInstance());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        anim = Math.min(anim + delta * 0.05f, 1f);
        drawHeader(ctx);
        nameField.render(ctx, mouseX, mouseY, delta);
        code.render(ctx, mouseX, mouseY, delta);
    }

    private void drawHeader(DrawContext ctx) {
        int w = width;
        int base = 0x44000000;
        ctx.fill(0, 0, w, 50, base);
        String t = Text.translatable("minecode.screen.editor").getString();
        int tw = textRenderer.getWidth(t);
        int x = w/2 - tw/2;
        int col = 0xFFFFFF | ((int)(anim*255) << 24);
        ctx.drawText(textRenderer, t, x, 16, col, false);
    }
}
