package johnsmith.configoverhauled.impl.client.gui.popup;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Detailed modal editor for {@link String} properties featuring a spacious text field
 * and live character count display.
 */
public class StringDetailPopup extends TextDetailPopup<String> {
    public StringDetailPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Property<String> property,
            @Nullable String initialText,
            boolean isLocked,
            Consumer<String> onApply
    ) {
        super(parentScreen, minecraft, property, initialText, isLocked, onApply);
    }

    @Override
    protected ValidationResult<String> validateInput(String text) {
        return ValidationResult.ok(text);
    }

    @Override
    protected String formatValue(String value) {
        return value != null ? value : "";
    }

    @Override
    protected Component getDefaultInfoText() {
        return Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": \"" + this.formatValue(this.property.defaultValue()) + "\""));
    }

    @Override
    protected Component getRightInfoText() {
        return Component.translatable("config_overhauled.popup.text.length", this.workingText.length());
    }
}
