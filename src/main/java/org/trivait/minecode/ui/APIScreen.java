// src/main/java/org/trivait/minecode/ui/APIScreen.java
package org.trivait.minecode.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.trivait.minecode.engine.functions.FunctionRegistry;
import org.trivait.minecode.engine.functions.MineFunction;

import java.util.ArrayList;
import java.util.List;

public class APIScreen extends Screen {
    private final Screen back;
    private float anim; // анимация появления
    private int scroll; // вертикальная прокрутка
    private List<MineFunction> functions;

    private static final int COLOR_PANEL = 0x66000000;
    private static final int COLOR_BORDER = 0xFFAA33; // оранжевая рамка
    private static final int COLOR_TITLE = 0xFFAA33;  // заголовок панели
    private static final int COLOR_DESC = 0xCCCCCC;   // описание
    private static final int COLOR_SIG = 0x33CCCC;    // сигнатуры — бирюзовый

    public APIScreen(Screen back) {
        super(Text.translatable("minecode.screen.api"));
        this.back = back;
    }

    @Override
    protected void init() {
        anim = 0f;
        functions = new ArrayList<>(FunctionRegistry.all());

        // Центрируем кнопку "Назад" сверху
        int bw = 100, bh = 20;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("minecode.button.back"),
                b -> this.client.setScreen(back)
        ).dimensions((this.width/2 - bw/2) + 400, 10, bw, bh).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int)(verticalAmount * 20);
        scroll = Math.max(0, scroll);
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // super в начале

        anim = Math.min(anim + delta * 0.06f, 1f);

        int w = width;
        int x = w/2 - 220;
        int y = 40 - scroll;

        // Заголовок экрана
        String title = Text.translatable("minecode.screen.api").getString();
        int tw = textRenderer.getWidth(title);
        // Панели функций
        for (MineFunction fn : functions) {
            int ph = 60 + fn.hints().size() * 14;
            int alpha = (int)(0x66 * anim) & 0xFF;
            int panelBg = (alpha << 24) | (COLOR_PANEL & 0x00FFFFFF);

            ctx.fill(x, y, x+440, y+ph, panelBg);
            drawBorder(ctx, x, y, 440, ph, COLOR_BORDER);

            // Заголовок и описание
            ctx.drawText(textRenderer, fn.id(), x+10, y+8, COLOR_TITLE, false);
            ctx.drawText(textRenderer, Text.translatable(fn.tutorialKey()), x+10, y+26, COLOR_DESC, false);

            // Сигнатуры/подсказки
            int yy = y + 44;
            for (String sig : fn.hints()) {
                ctx.drawText(textRenderer, "• " + sig, x+16, yy, COLOR_SIG, false);
                yy += 14;
            }

            y += ph + 12;
            ctx.drawText(textRenderer, title, w/2 - tw/2, 20, 0xFFFFFF | ((int)(anim*255) << 24), false);
        }
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x+w, y+1, col);
        ctx.fill(x, y+h-1, x+w, y+h, col);
        ctx.fill(x, y, x+1, y+h, col);
        ctx.fill(x+w-1, y, x+w, y+h, col);
    }
}
