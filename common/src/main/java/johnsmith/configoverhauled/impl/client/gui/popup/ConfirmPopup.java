package johnsmith.configoverhauled.impl.client.gui.popup;

import johnsmith.configoverhauled.api.client.gui.popup.AbstractConfigPopup;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Compact modal confirmation dialog built on {@link AbstractConfigPopup}.
 */
public class ConfirmPopup extends AbstractConfigPopup<Void> {
    private static final int MESSAGE_COLOR = 0xFFE0E0E0;

    private final Component message;
    private final Component confirmLabel;
    private List<FormattedCharSequence> wrappedMessage = List.of();

    public ConfirmPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Component title,
            Component message,
            Component confirmLabel,
            Runnable onConfirm
    ) {
        super(parentScreen, minecraft, title, null, false, ignored -> onConfirm.run());
        this.message = message;
        this.confirmLabel = confirmLabel;
    }

    @Override
    protected int getPreferredWidth() {
        return 300;
    }

    @Override
    protected int getPreferredHeight() {
        return 124;
    }

    @Override
    protected boolean showResetButton() {
        return false;
    }

    @Override
    protected Component getApplyButtonLabel() {
        return this.confirmLabel;
    }

    @Override
    protected void initContent(int left, int top, int width, int height) {
        this.wrappedMessage = this.font.split(this.message, Math.max(40, width));
    }

    @Override
    protected void extractBody(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.wrappedMessage.isEmpty()) {
            return;
        }
        int lineStep = this.font.lineHeight + 3;
        int totalHeight = this.wrappedMessage.size() * lineStep - 3;
        int y = this.contentTop + Math.max(0, (this.contentHeight - totalHeight) / 2);

        for (FormattedCharSequence line : this.wrappedMessage) {
            guiGraphics.text(this.font, line, this.contentLeft, y, MESSAGE_COLOR, false);
            y += lineStep;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
                && this.getFocusedWidget() != this.cancelButton) {
            this.applyAndClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
