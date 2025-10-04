package org.trivait.minecode.ui.style;

import net.minecraft.client.gui.DrawContext;

public class UIStyle {
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        int bg = 0x33000000;
        ctx.fill(x, y, x+w, y+h, bg);
        int border = 0x2200D0FF;
        ctx.fill(x, y, x+w, y+1, border);
        ctx.fill(x, y+h-1, x+w, y+h, border);
        ctx.fill(x, y, x+1, y+h, border);
        ctx.fill(x+w-1, y, x+w, y+h, border);
    }

    public static void drawGradient(DrawContext ctx, int x, int y, int w, int h, int c1, int c2) {
        ctx.fillGradient(x, y, x+w, y+h, c1, c2);
    }
}
