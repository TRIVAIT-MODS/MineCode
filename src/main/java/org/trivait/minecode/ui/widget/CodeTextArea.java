// src/main/java/org/trivait/minecode/ui/widget/CodeTextArea.java
package org.trivait.minecode.ui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.trivait.minecode.engine.Parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeTextArea extends ClickableWidget {
    private final TextRenderer tr;
    private String text = "";
    private int caretLine = 0;
    private int caretCol = 0;
    private int scroll = 0;
    private boolean focused;

    private java.util.function.Function<String, List<String>> hintsProvider;
    private List<String> hints = new ArrayList<>();
    private int hintIndex = -1;

    // Undo/Redo
    private final Deque<State> undoStack = new ArrayDeque<>();
    private final Deque<State> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 100;

    // Ошибки
    private final Map<Integer, String> errors = new HashMap<>();

    // Выделение текста
    private boolean selecting = false;
    private int selStartLine = -1, selStartCol = -1;
    private int selEndLine = -1, selEndCol = -1;

    // Цвета
    private static final int COLOR_BG = 0x88000000;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_COMMENT = 0x888888;
    private static final int COLOR_FUNC = 0xFFAA33;
    private static final int COLOR_VAR = 0x33CCCC;
    private static final int COLOR_BUILTIN = 0x55DD55;
    private static final int COLOR_VALUE = 0x00FFFF;
    private static final int COLOR_LINE_NUM = 0xAAAAAA;
    private static final int COLOR_ERROR = 0xFF4444;
    private static final int COLOR_CARET = 0xCC66BFFF;
    private static final int COLOR_SELECTION = 0x553399FF;

    public CodeTextArea(TextRenderer tr, int x, int y, int w, int h) {
        super(x, y, w, h, Text.empty());
        this.tr = tr;
        pushUndo();
    }

    public void setText(String t) {
        this.text = t == null ? "" : t.replace("\r\n", "\n");
        clampCaret();
        refreshHints();
        analyzeErrors();
    }

    public String getText() {
        return text;
    }

    public void setHintsProvider(java.util.function.Function<String, List<String>> hintsProvider) {
        this.hintsProvider = hintsProvider;
        refreshHints();
    }

    private List<String> lines() {
        return new ArrayList<>(Arrays.asList(text.split("\n", -1)));
    }

    private void setLines(List<String> ls) {
        this.text = String.join("\n", ls);
        refreshHints();
        analyzeErrors();
    }

    private void analyzeErrors() {
        errors.clear();
        String full = this.text;
        if (full.trim().isEmpty()) return;
        try {
            Parser.parse(full);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            // Попробуем извлечь номер строки из "Error at line X:"
            int lineIdx = 0;
            Matcher m = Pattern.compile("line\\s+(\\d+)").matcher(msg.toLowerCase(Locale.ROOT));
            if (m.find()) {
                try {
                    int ln1 = Integer.parseInt(m.group(1)); // 1-based
                    lineIdx = Math.max(0, ln1 - 1);         // в errors — 0-based
                } catch (Exception ignore) {}
            }
            errors.put(lineIdx, msg);
        }
    }


    @Override
    public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(getX(), getY(), getX()+width, getY()+height, COLOR_BG);

        List<String> ls = lines();
        int lineHeight = tr.fontHeight + 2;
        int visible = height / lineHeight;

        for (int i=0; i<visible && i+scroll < ls.size(); i++) {
            int lineNum = i+scroll;
            String s = ls.get(lineNum);
            int y = getY() + 4 + i*lineHeight;

            // Подсветка выделения
            if (selecting && selStartLine >= 0 && selEndLine >= 0) {
                int startLine = Math.min(selStartLine, selEndLine);
                int endLine = Math.max(selStartLine, selEndLine);
                int startCol = (selStartLine < selEndLine) ? selStartCol : selEndCol;
                int endCol = (selStartLine < selEndLine) ? selEndCol : selStartCol;

                for (int ln = startLine; ln <= endLine; ln++) {
                    if (ln < scroll || ln >= scroll + visible) continue;
                    String line = ls.get(ln);
                    int yy = getY() + 4 + (ln - scroll) * lineHeight;
                    int x0 = getX() + 40;
                    int from = (ln == startLine ? startCol : 0);
                    int to = (ln == endLine ? endCol : line.length());
                    from = Math.max(0, Math.min(from, line.length()));
                    to = Math.max(0, Math.min(to, line.length()));
                    int xStart = x0 + tr.getWidth(line.substring(0, from));
                    int xEnd = x0 + tr.getWidth(line.substring(0, to));
                    if (xEnd > xStart) {
                        ctx.fill(xStart, yy, xEnd, yy + tr.fontHeight, COLOR_SELECTION);
                    }
                }
            }

            // Номер строки
            ctx.drawText(tr, String.valueOf(lineNum+1), getX()+2, y, COLOR_LINE_NUM, false);

            // Подсветка ошибок
            boolean hasError = errors.containsKey(lineNum);
            int base = hasError ? COLOR_ERROR : COLOR_TEXT;
            if (s.trim().startsWith("#")) base = COLOR_COMMENT;

            drawColoredLine(ctx, s, getX()+40, y, base);

            // Каретка
            if (focused && lineNum == caretLine) {
                int cx = getX()+40 + tr.getWidth(s.substring(0, Math.min(caretCol, s.length())));
                ctx.fill(cx, y, cx+1, y + tr.fontHeight, COLOR_CARET);
            }

            // Тултип ошибки
            if (hasError && mx >= getX() && mx <= getX()+width && my >= y && my <= y+lineHeight) {
                ctx.drawTooltip(tr, Text.literal(errors.get(lineNum)), mx, my);
            }
        }

        // Подсказки снизу (макс 7)
        if (focused && hints != null && !hints.isEmpty()) {
            int showCount = Math.min(7, hints.size());
            int bx = getX() + 8;
            int by = getY() + height - 8 - showCount * lineHeight;
            ctx.fill(bx-6, by-6, bx + 240, by + showCount*lineHeight + 6, 0xAA222222);
            for (int i=0; i<showCount; i++) {
                String h = hints.get(i);
                int col = (i == hintIndex) ? COLOR_FUNC : COLOR_TEXT;
                ctx.drawText(tr, h, bx, by + i*lineHeight, col, false);
            }
        }

        // Счётчик ошибок
        String errText = "Errors: " + errors.size();
        int tw = tr.getWidth(errText);
        ctx.drawText(tr, errText, getX()+width - tw - 6, getY()+4, errors.isEmpty() ? COLOR_BUILTIN : COLOR_ERROR, false);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int lineHeight = tr.fontHeight + 2;
        int totalLines = lines().size();
        int visible = height / lineHeight;
        int maxScroll = Math.max(0, totalLines - visible);
        scroll -= (int)verticalAmount;
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= getX() && mx <= getX()+width && my >= getY() && my <= getY()+height) {
            focused = true;
            int lineHeight = tr.fontHeight + 2;
            int idx = (int)((my - getY() - 4) / lineHeight);
            List<String> ls = lines();
            caretLine = Math.max(0, Math.min(ls.size()-1, idx + scroll));
            String s = ls.get(caretLine);
            int relx = (int)(mx - getX() - 40);
            if (relx < 0) relx = 0;
            int c = 0;
            while (c < s.length() && tr.getWidth(s.substring(0, c)) < relx) c++;
            caretCol = c;

            // начало выделения
            selecting = true;
            selStartLine = caretLine;
            selStartCol = caretCol;
            selEndLine = caretLine;
            selEndCol = caretCol;
            return true;
        }
        focused = false;
        return false;
    }

    private void drawColoredLine(DrawContext ctx, String s, int x, int y, int base) {
        String[] tokens = s.split(" ");
        int curX = x;
        for (String t : tokens) {
            int col = base;
            String up = t.toUpperCase();

            // значения в кавычках или числа
            if (isQuoted(t) || isNumber(t)) {
                col = COLOR_VALUE;
            }
            // переменные
            else if (t.startsWith("$")) {
                col = COLOR_VAR;
            }
            // встроенные конструкции
            else if (up.equals("IF") || up.equals("BLOCK") || up.equals("LOOP")) {
                col = COLOR_BUILTIN;
            }
            // функции
            else if (up.equals("SAY") || up.equals("WAIT") || up.equals("LOOK") || up.startsWith("WALK")) {
                col = COLOR_FUNC;
            }

            ctx.drawText(tr, t + " ", curX, y, col, false);
            curX += tr.getWidth(t + " ");
        }
    }

    private boolean hasSelection() {
        return selecting && selStartLine >= 0 && selEndLine >= 0 &&
                (selStartLine != selEndLine || selStartCol != selEndCol);
    }

    private void clearSelection() {
        selecting = false;
        selStartLine = selEndLine = -1;
        selStartCol = selEndCol = -1;
    }

    /** Удаляет выделенный диапазон (многострочный, с безопасными индексами) и ставит каретку на начало диапазона */
    private void deleteSelection() {
        List<String> ls = lines();
        if (ls.isEmpty()) { clearSelection(); return; }

        int startLine = Math.min(selStartLine, selEndLine);
        int endLine = Math.max(selStartLine, selEndLine);
        int startCol = (selStartLine < selEndLine) ? selStartCol : selEndCol;
        int endCol   = (selStartLine < selEndLine) ? selEndCol   : selStartCol;

        String lineStart = ls.get(startLine);
        String lineEnd   = ls.get(endLine);

        int safeStartCol = Math.max(0, Math.min(startCol, lineStart.length()));
        int safeEndCol   = Math.max(0, Math.min(endCol,   lineEnd.length()));

        String first = lineStart.substring(0, safeStartCol);
        String last  = lineEnd.substring(safeEndCol);

        List<String> newLs = new ArrayList<>();
        for (int i = 0; i < startLine; i++) newLs.add(ls.get(i));
        newLs.add(first + last);
        for (int i = endLine + 1; i < ls.size(); i++) newLs.add(ls.get(i));

        caretLine = startLine;
        caretCol  = first.length();
        setLines(newLs);
        clearSelection();
    }


    // Проверка: строка в кавычках
    private boolean isQuoted(String t) {
        return t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"");
    }

    // Проверка: число (целое или с плавающей точкой)
    private boolean isNumber(String t) {
        if (t == null || t.isEmpty()) return false;
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }



    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!selecting) return false;
        int lineHeight = tr.fontHeight + 2;
        int idx = (int)((my - getY() - 4) / lineHeight);
        List<String> ls = lines();
        selEndLine = Math.max(0, Math.min(ls.size()-1, idx + scroll));
        String s = ls.get(selEndLine);
        int relx = (int)(mx - getX() - 40);
        if (relx < 0) relx = 0;
        int c = 0;
        while (c < s.length() && tr.getWidth(s.substring(0, c)) < relx) c++;
        selEndCol = c;
        caretLine = selEndLine;
        caretCol = selEndCol;
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        List<String> ls = lines();
        String cur = ls.isEmpty() ? "" : ls.get(caretLine);

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        // Undo/Redo
        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) { undo(); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) { redo(); return true; }

        // Выделить всё
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            selStartLine = 0;
            selStartCol = 0;
            selEndLine = ls.size() - 1;
            selEndCol = ls.get(selEndLine).length();
            caretLine = selEndLine;
            caretCol = selEndCol;
            selecting = true;
            int lineHeight = tr.fontHeight + 2;
            int visible = height / lineHeight;
            scroll = Math.max(0, ls.size() - visible);
            return true;
        }

        // Копирование
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            if (selStartLine >= 0 && selEndLine >= 0) {
                int startLine = Math.min(selStartLine, selEndLine);
                int endLine = Math.max(selStartLine, selEndLine);
                int startCol = (selStartLine < selEndLine) ? selStartCol : selEndCol;
                int endCol = (selStartLine < selEndLine) ? selEndCol : selStartCol;

                StringBuilder sb = new StringBuilder();
                for (int i = startLine; i <= endLine; i++) {
                    String line = ls.get(i);
                    int from = (i == startLine ? startCol : 0);
                    int to = (i == endLine ? endCol : line.length());
                    from = Math.max(0, Math.min(from, line.length()));
                    to = Math.max(0, Math.min(to, line.length()));
                    if (from < to) sb.append(line, from, to);
                    if (i < endLine) sb.append("\n");
                }
                MinecraftClient.getInstance().keyboard.setClipboard(sb.toString());
            } else {
                MinecraftClient.getInstance().keyboard.setClipboard(text);
            }
            return true;
        }

        // Вырезание
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            if (selStartLine >= 0 && selEndLine >= 0) {
                pushUndo();
                int startLine = Math.min(selStartLine, selEndLine);
                int endLine = Math.max(selStartLine, selEndLine);
                int startCol = (selStartLine < selEndLine) ? selStartCol : selEndCol;
                int endCol = (selStartLine < selEndLine) ? selEndCol : selStartCol;

                // Сохраняем в буфер
                StringBuilder sb = new StringBuilder();
                for (int i = startLine; i <= endLine; i++) {
                    String line = ls.get(i);
                    int from = (i == startLine ? startCol : 0);
                    int to = (i == endLine ? endCol : line.length());
                    from = Math.max(0, Math.min(from, line.length()));
                    to = Math.max(0, Math.min(to, line.length()));
                    if (from < to) sb.append(line, from, to);
                    if (i < endLine) sb.append("\n");
                }
                MinecraftClient.getInstance().keyboard.setClipboard(sb.toString());

                // Удаляем выделенный диапазон
                String lineStart = ls.get(startLine);
                String lineEnd = ls.get(endLine);

                int safeStartCol = Math.max(0, Math.min(startCol, lineStart.length()));
                int safeEndCol = Math.max(0, Math.min(endCol, lineEnd.length()));

                String first = lineStart.substring(0, safeStartCol);
                String last = lineEnd.substring(safeEndCol);
                List<String> newLs = new ArrayList<>();
                for (int i = 0; i < startLine; i++) newLs.add(ls.get(i));
                newLs.add(first + last);
                for (int i = endLine+1; i < ls.size(); i++) newLs.add(ls.get(i));

                caretLine = startLine;
                caretCol = first.length();
                setLines(newLs);

                // сброс выделения
                selecting = false;
                selStartLine = selEndLine = -1;
                selStartCol = selEndCol = -1;
                return true;
            } else {
                MinecraftClient.getInstance().keyboard.setClipboard(text);
                pushUndo();
                setText("");
                caretLine = 0; caretCol = 0; scroll = 0;
                return true;
            }
        }

        // Вставка
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                pushUndo();
                insertText(clip);
            }
            return true;
        }
        // Стрелки и редактирование
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> { caretCol = Math.max(0, caretCol - 1); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { caretCol = Math.min(cur.length(), caretCol + 1); return true; }
            case GLFW.GLFW_KEY_UP -> { moveUp(); return true; }
            case GLFW.GLFW_KEY_DOWN -> { moveDown(); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                pushUndo();
                if (hasSelection()) {
                    deleteSelection();
                } else {
                    backspace();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> { pushUndo(); delete(); return true; }
            case GLFW.GLFW_KEY_ENTER -> { pushUndo(); splitLine(); return true; }
            case GLFW.GLFW_KEY_TAB -> {
                if (!hints.isEmpty()) {
                    pushUndo();
                    String replacement = hints.get(0);
                    replaceCurrentToken(replacement);
                } else {
                    pushUndo();
                    insertText("    ");
                }
                return true;
            }
        }
        return false;
    }

    private void replaceCurrentToken(String replacement) {
        List<String> ls = lines();
        String cur = ls.get(caretLine);
        int start = caretCol;
        while (start > 0 && !Character.isWhitespace(cur.charAt(start-1))) start--;
        int end = caretCol;
        while (end < cur.length() && !Character.isWhitespace(cur.charAt(end))) end++;
        String newLine = cur.substring(0, start) + replacement + cur.substring(end);
        ls.set(caretLine, newLine);
        caretCol = start + replacement.length();
        setLines(ls);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr == '\r' || chr == '\n') return false;
        pushUndo();
        insertText(String.valueOf(chr));
        return true;
    }

    private void moveUp() {
        List<String> ls = lines();
        if (caretLine > 0) {
            caretLine--;
            caretCol = Math.min(caretCol, ls.get(caretLine).length());
            scroll = Math.max(0, Math.min(scroll, caretLine));
        }
    }

    private void moveDown() {
        List<String> ls = lines();
        if (caretLine < ls.size()-1) {
            caretLine++;
            caretCol = Math.min(caretCol, ls.get(caretLine).length());
            int lineHeight = tr.fontHeight + 2;
            int visible = height / lineHeight;
            if (caretLine - scroll >= visible) {
                scroll = Math.min(scroll + 1, Math.max(0, ls.size() - visible));
            }
        }
    }

    private void backspace() {
        List<String> ls = lines();
        if (ls.isEmpty()) return;
        if (caretCol > 0) {
            String cur = ls.get(caretLine);
            int safeCol = Math.min(caretCol, cur.length());
            String newLine = cur.substring(0, safeCol-1) + cur.substring(safeCol);
            ls.set(caretLine, newLine);
            caretCol--;
            setLines(ls);
        } else if (caretLine > 0) {
            String prev = ls.get(caretLine-1);
            String cur = ls.get(caretLine);
            int prevLen = prev.length();
            ls.set(caretLine-1, prev + cur);
            ls.remove(caretLine);
            caretLine--;
            caretCol = prevLen;
            setLines(ls);
        }
    }

    private void delete() {
        List<String> ls = lines();
        if (ls.isEmpty()) return;
        String cur = ls.get(caretLine);
        if (caretCol < cur.length()) {
            String newLine = cur.substring(0, caretCol) + cur.substring(caretCol+1);
            ls.set(caretLine, newLine);
            setLines(ls);
        } else if (caretLine < ls.size()-1) {
            String next = ls.get(caretLine+1);
            ls.set(caretLine, cur + next);
            ls.remove(caretLine+1);
            setLines(ls);
        }
    }

    private void splitLine() {
        List<String> ls = lines();
        String cur = ls.get(caretLine);
        int safeCol = Math.min(caretCol, cur.length());
        String left = cur.substring(0, safeCol);
        String right = cur.substring(safeCol);
        ls.set(caretLine, left);
        ls.add(caretLine+1, right);
        caretLine++;
        caretCol = 0;
        setLines(ls);
    }

    private void insertText(String s) {
        List<String> ls = lines();
        if (ls.isEmpty()) ls = new ArrayList<>(List.of(""));
        String cur = ls.get(caretLine);
        int safeCol = Math.min(caretCol, cur.length());
        String newLine = cur.substring(0, safeCol) + s + cur.substring(safeCol);
        ls.set(caretLine, newLine);
        caretCol = safeCol + s.length();
        setLines(ls);
    }

    private void refreshHints() {
        if (hintsProvider == null) { hints = List.of(); return; }
        String prefix = currentToken();
        hints = hintsProvider.apply(prefix);
        hintIndex = hints.isEmpty() ? -1 : 0;
    }

    private String currentToken() {
        List<String> ls = lines();
        if (ls.isEmpty()) return "";
        String cur = ls.get(caretLine);
        int start = Math.max(0, Math.min(caretCol, cur.length()));
        int left = start;
        while (left > 0 && !Character.isWhitespace(cur.charAt(left-1))) left--;
        return cur.substring(left, start);
    }

    private void pushUndo() {
        undoStack.push(new State(text, caretLine, caretCol, scroll));
        while (undoStack.size() > MAX_HISTORY) undoStack.removeLast();
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.size() <= 1) return;
        State current = new State(text, caretLine, caretCol, scroll);
        State prev = undoStack.pop();
        State target = undoStack.peek();
        if (target != null) {
            redoStack.push(current);
            applyState(target);
        } else {
            applyState(prev);
        }
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        State next = redoStack.pop();
        undoStack.push(next);
        applyState(next);
    }

    private void applyState(State s) {
        this.text = s.text;
        this.caretLine = s.line;
        this.caretCol = s.col;
        this.scroll = s.scroll;
        clampCaret();
        refreshHints();
        analyzeErrors();
    }

    private void clampCaret() {
        List<String> ls = lines();
        caretLine = Math.max(0, Math.min(caretLine, ls.size()-1));
        caretCol = Math.max(0, Math.min(caretCol, ls.get(caretLine).length()));
    }

    private record State(String text, int line, int col, int scroll) {}
}
