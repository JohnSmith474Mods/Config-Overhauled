package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.RenderableIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class AbstractRegistryEntry<T, V> extends OptionEntry<V, Button> {
    protected final Registry<T> registry;
    protected final Function<T, RenderableIcon> iconProvider;
    protected final Function<T, Component> nameProvider;
    private boolean widgetInitialized = false;

    public AbstractRegistryEntry(Property<V> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged, Registry<T> registry, Function<T, RenderableIcon> iconProvider, Function<T, Component> nameProvider) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.registry = registry;
        this.iconProvider = iconProvider;
        this.nameProvider = nameProvider;
    }

    @Override
    public void extractContent(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        if (!this.widgetInitialized) {
            this.updateWidgetValue();
            this.widgetInitialized = true;
        }
        super.extractContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);
    }

    @Override
    protected Button createWidget() {
        return Button.builder(Component.empty(), button -> this.openSelectionScreen()).bounds(0, 0, 75, 20).build();
    }

    protected void applySelection(V newValue) {
        this.setValue(newValue);
        this.updateWidgetValue();
    }

    protected abstract void openSelectionScreen();
}