package org.trivait.minecode.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.trivait.minecode.engine.ScriptEngine;

import java.util.*;

public class CustomTextAreaWidget extends ClickableWidget implements Element, Selectable {
    private final int posX, posY, areaWidth, areaHeight;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final List<String> lines = new ArrayList<>();

    private final List<String> knownCommands = List.of(
            "say", "run", "run.sprint", "run.left", "run.right", "run.back",
            "wait", "jump", "clickLeft", "clickRight", "breakBlock",
            "cameraRotation", "setCameraRotation", "setHotBar", "break",
            "int", "string", "bool", "Block", "for", "if", "follow"
    );
    private final Set<String> knownVariables = new HashSet<>();

    private int scrollOffset = 0;
    private boolean isFocused = true;
    private int cursorLine = 0;
    private int cursorCol = 0;

    private final Set<Integer> errorLines = new HashSet<>();


    private long cursorTimer = 0;
    private boolean cursorVisible = true;

    private List<String> suggestions = new ArrayList<>();
    private static final int MAX_SUGGESTIONS = 5;

    private static final int COLOR_CMD = 0xFFFFA500;
    private static final int COLOR_VAR = 0xFF80C0FF;
    private static final int COLOR_DEF = 0xFFFFFFFF;

    public CustomTextAreaWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal(""));
        this.posX = x;
        this.posY = y;
        this.areaWidth = width;
        this.areaHeight = height;
        lines.add("");
    }

    private void validateLines() {
        errorLines.clear();
        for (int i = 0; i < lines.size(); i++) {
            if (!isValidLine(lines.get(i))) {
                errorLines.add(i);
            }
        }
    }


    private void updateKnownVariables() {
        knownVariables.clear();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("int ") || trimmed.startsWith("string ") || trimmed.startsWith("bool ") || trimmed.startsWith("Block ")) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    knownVariables.add(parts[1].replace("=", "").trim());
                }
            } else if (trimmed.contains("=")) {
                String name = trimmed.split("=")[0].trim();
                knownVariables.add(name);
            }
        }
    }

    private void updateSuggestions() {
        if (cursorLine < lines.size()) {
            String current = lines.get(cursorLine).trim();
            String word = current.split("\\s+")[0];
            suggestions = new ArrayList<>();

            for (String cmd : knownCommands) {
                if (cmd.startsWith(word)) suggestions.add(cmd);
            }

            for (String var : knownVariables) {
                if (var.startsWith(word)) suggestions.add(var);
            }
        }
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            lines.add(cursorLine + 1, "");
            cursorLine++;
            cursorCol = 0;
            scrollToBottom();
            updateSuggestions();
            updateKnownVariables();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorLine < lines.size()) {
                String line = lines.get(cursorLine);
                if (cursorCol > 0) {
                    lines.set(cursorLine, line.substring(0, cursorCol - 1) + line.substring(cursorCol));
                    cursorCol--;
                } else if (cursorLine > 0) {
                    String prev = lines.get(cursorLine - 1);
                    lines.set(cursorLine - 1, prev + lines.get(cursorLine));
                    lines.remove(cursorLine);
                    cursorLine--;
                    cursorCol = prev.length();
                }
            }
            validateLines();
            updateSuggestions();
            updateKnownVariables();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (!suggestions.isEmpty()) {
                String current = lines.get(cursorLine);
                String typed = current.substring(0, cursorCol);
                String prefix = typed.split("\\s+")[typed.split("\\s+").length - 1];
                String suggestion = suggestions.get(0);

                int start = typed.lastIndexOf(prefix);
                String before = current.substring(0, start);
                String after = current.substring(cursorCol);
                String newLine = before + suggestion + after;

                lines.set(cursorLine, newLine);
                cursorCol = before.length() + suggestion.length();
                updateSuggestions();
                updateKnownVariables();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
            String pasted = client.keyboard.getClipboard();
            for (char c : pasted.toCharArray()) {
                if (c == '\n' || c == '\r') {
                    lines.add(++cursorLine, "");
                    cursorCol = 0;
                } else {
                    String line = lines.get(cursorLine);
                    lines.set(cursorLine, line.substring(0, cursorCol) + c + line.substring(cursorCol));
                    cursorCol++;
                }
            }
            scrollToBottom();
            updateKnownVariables();
            updateSuggestions();
            validateLines();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorCol > 0) cursorCol--;
            else if (cursorLine > 0) {
                cursorLine--;
                cursorCol = lines.get(cursorLine).length();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorCol < lines.get(cursorLine).length()) cursorCol++;
            else if (cursorLine + 1 < lines.size()) {
                cursorLine++;
                cursorCol = 0;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (cursorLine > 0) {
                cursorLine--;
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (cursorLine + 1 < lines.size()) {
                cursorLine++;
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            }
            return true;
        }



        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!isFocused || chr == '\n' || chr == '\r') return false;

        String line = lines.get(cursorLine);
        lines.set(cursorLine, line.substring(0, cursorCol) + chr + line.substring(cursorCol));
        cursorCol++;
        validateLines();
        scrollToBottom();
        updateSuggestions();
        updateKnownVariables();
        return true;
    }

    public void setText(String text) {
        lines.clear();
        Collections.addAll(lines, text.split("\n", -1));
        cursorLine = lines.size() - 1;
        cursorCol = lines.get(cursorLine).length();
        scrollToBottom();
        updateSuggestions();
        updateKnownVariables();
    }

    public String getText() {
        return String.join("\n", lines);
    }

    private void scrollToBottom() {
        scrollOffset = Math.max(0, lines.size() - getMaxVisibleLines());
    }

    private int getMaxVisibleLines() {
        return areaHeight / client.textRenderer.fontHeight;
    }
    public boolean isValidLine(String line) {
        if (line == null || line.trim().isEmpty()) return true;

        String trimmed = line.trim();

        if (trimmed.startsWith("//")) return true;

        List<String> commands = List.of(
                "say", "run", "run.sprint", "run.left", "run.right", "run.back",
                "wait", "jump", "clickLeft", "clickRight", "breakBlock",
                "cameraRotation", "setCameraRotation", "setHotBar", "break",
                "for", "if", "follow"
        );
        for (String cmd : commands) {
            if (trimmed.startsWith(cmd + " ") || trimmed.equals(cmd)) return true;
        }

        if (trimmed.matches("^(int|string|bool|Block)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*(=.+)?$")) return true;

        if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=.+$")) return true;


        if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\)$")) return true;


        if (trimmed.matches("^player\\.(x|y|z|jump|isSneaking|underBlock).*")) return true;

        if (trimmed.matches("}")) return true;


        if (trimmed.matches("^Block\\[\\d+\\]\\[\\d+\\]\\[\\d+]\\s*=.+$")) return true;

        return false;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer tr = client.textRenderer;
        int lineHeight = tr.fontHeight;
        String errLabel = Text.translatable("minecode.errors.label").getString() + " " + errorLines.size();


        context.fill(posX, posY, posX + areaWidth, posY + areaHeight, 0x66000000);

        int visibleLines = getMaxVisibleLines();

        for (int i = 0; i < visibleLines; i++) {
            int index = i + scrollOffset;
            if (index >= lines.size()) break;

            int y = posY + i * lineHeight;
            int errColor = errorLines.size() == 0 ? 0xFF00FF00 : 0xFFFF0000;

            if (!isValidLine(lines.get(index))) {
                context.fill(posX, y, posX + areaWidth, y + lineHeight, 0x44FF0000);
            }

            String[] words = lines.get(index).split(" ");
            int x = posX + 4;
            for (String word : words) {
                int color = COLOR_DEF;
                if (knownCommands.contains(word)) color = COLOR_CMD;
                else if (knownVariables.contains(word)) color = COLOR_VAR;

                context.drawText(tr, word + " ", x, y, color, false);
                x += tr.getWidth(word + " ");
            }
            context.drawText(tr, errLabel, posX + areaWidth - 60, posY + 6, errColor, false);
        }



        cursorTimer++;
        if (cursorTimer % 15 == 0) cursorVisible = !cursorVisible;

        if (cursorVisible && isFocused) {
            if (cursorLine >= scrollOffset && cursorLine < scrollOffset + visibleLines && cursorLine < lines.size()) {
                int y = posY + (cursorLine - scrollOffset) * lineHeight;
                String line = lines.get(cursorLine);
                int x = posX + 4 + tr.getWidth(line.substring(0, Math.min(cursorCol, line.length())));
                context.fill(x, y, x + 1, y + lineHeight, 0xFFFFFFFF);
            }
        }

        if (!suggestions.isEmpty()) {
            int sx = posX + 6;
            int sy = posY + areaHeight + 4;
            for (int i = 0; i < Math.min(MAX_SUGGESTIONS, suggestions.size()); i++) {
                String suggestion = suggestions.get(i);
                context.drawText(tr, suggestion, sx, sy + i * (tr.fontHeight + 2), 0xAAAAAA, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int localY = (int) mouseY - posY;
        int lineIndex = scrollOffset + localY / client.textRenderer.fontHeight;
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            String line = lines.get(lineIndex);
            int x = posX + 4;
            int col = 0;
            while (col < line.length() && x < mouseX) {
                x += client.textRenderer.getWidth(String.valueOf(line.charAt(col)));
                col++;
            }
            cursorLine = lineIndex;
            cursorCol = col;
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= posX && mouseX < posX + areaWidth && mouseY >= posY && mouseY < posY + areaHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollOffset -= verticalAmount > 0 ? 1 : -1;
        scrollOffset = Math.max(0, Math.min(scrollOffset, lines.size() - getMaxVisibleLines()));
        return true;
    }

    @Override public boolean isFocused() { return isFocused; }
    @Override public void setFocused(boolean focused) { this.isFocused = focused; }
    @Override public SelectionType getType() { return SelectionType.NONE; }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
