package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public abstract class AbstractTextEntry<T> extends OptionEntry<T, EditBox> {
    public AbstractTextEntry(Property<T> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
    }

    @Override
    protected EditBox createWidget() {
        EditBox box = new EditBox(this.minecraft.font, 0, 0, 75, 20, Component.empty()) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (!focused) {
                    applyInput(this.getValue());
                }
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD ENTER
                    applyInput(this.getValue());
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        box.setMaxLength(32767);

        this.setupEditBox(box);

        return box;
    }

    /**
     * Attaches live responders or input filters to the EditBox (e.g., coloring text red on invalid input).
     */
    protected abstract void setupEditBox(EditBox box);

    /**
     * Called when the user commits their input (Presses Enter or clicks away).
     */
    protected abstract void applyInput(String input);

    /**
     * Utility method to revert the UI if the committed input was invalid.
     */
    protected void restoreCurrentValue() {
        this.updateWidgetValue();
    }
}