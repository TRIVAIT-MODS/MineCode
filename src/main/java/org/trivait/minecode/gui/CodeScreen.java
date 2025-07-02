package org.trivait.minecode.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.trivait.minecode.util.ConfigManager;
import org.trivait.minecode.util.KeyBindings;

public class CodeScreen extends Screen {
    private CustomTextAreaWidget codeEditor;


    public CodeScreen() {
        super(Text.translatable("screen.autocode.title"));
    }

    public String getScriptText() {
        return codeEditor.getText();
    }

    @Override
    protected void init() {
        int padding = 20;
        int areaWidth = this.width - padding * 2;
        int areaHeight = this.height - 100;
        int posX = 20;
        int posY = 28;
        int buttonSize = 18;
        int gap = 4;

        int buttonY = posY + gap;

        int copyX  = posX + areaWidth - buttonSize * 3 - gap * 2;
        int linkX  = copyX + buttonSize + gap;
        int clearX = linkX + buttonSize + gap;


        codeEditor = new CustomTextAreaWidget(padding, 50, areaWidth, areaHeight);
        codeEditor.setText(ConfigManager.loadScript());
        this.addDrawableChild(codeEditor);

        int btnSize = 16;
        int spacing = 4;
        int startX = this.width - 20 - (btnSize + spacing) * 2;
        int topY = 36;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("📋"),
                        button -> {
                            MinecraftClient.getInstance().keyboard.setClipboard(codeEditor.getText());
                        }
                ).dimensions(copyX, buttonY, buttonSize, buttonSize)
                .tooltip(Tooltip.of(Text.translatable("minecode.button.copy.tooltip")))
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("🌐"),
                        button -> {
                            String link = "https://pygrammerik.github.io/docsminecode"; // заменяем на нужную ссылку
                            if (link != null && !link.isEmpty()) {
                                Util.getOperatingSystem().open(link);
                            }
                        }
                ).dimensions(linkX,  buttonY, buttonSize, buttonSize)
                .tooltip(Tooltip.of(Text.translatable("minecode.button.link.tooltip")))
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.autocode.del"),
                        button -> {
                            MinecraftClient.getInstance().setScreen(new ConfirmClearScreen(
                                    () -> {
                                        codeEditor.setText("");
                                        MinecraftClient.getInstance().keyboard.setClipboard("");
                                        MinecraftClient.getInstance().setScreen(this); // вернуться обратно
                                    },
                                    () -> MinecraftClient.getInstance().setScreen(this)
                            ));
                        }
                ).dimensions(clearX, buttonY, buttonSize, buttonSize)
                .tooltip(Tooltip.of(Text.translatable("minecode.button.clear.tooltip")))
                .build());
        ButtonWidget doneButton = ButtonWidget.builder(
                Text.translatable("screen.autocode.done"),
                btn -> {
                    ConfigManager.saveScript(codeEditor.getText());
                    this.client.setScreen(null);
                }
        ).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build();
        this.addDrawableChild(doneButton);

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_CODE_MENU.matchesKey(keyCode, scanCode)) {
            ConfigManager.saveScript(codeEditor.getText());
            this.client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
