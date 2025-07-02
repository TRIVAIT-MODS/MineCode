package org.trivait.minecode.gui;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;

public class ConfirmClearScreen extends ConfirmScreen {
    private final Runnable onYes;
    private final Runnable onNo;

    public ConfirmClearScreen(Runnable onYes, Runnable onNo) {
        super(
                confirmed -> {
                    if (confirmed) onYes.run(); else onNo.run();
                },
                Text.translatable("screen.autocode.confirm_delete"),
                Text.translatable("screen.autocode.confirm_delete.message"),
                Text.translatable("screen.autocode.yes"),
                Text.translatable("screen.autocode.no")
        );
        this.onYes = onYes;
        this.onNo = onNo;
    }
}
