package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public abstract class AbstractRegistryEntry<T, V> extends OptionEntry<V, Button> {
    protected final Registry<T> registry;
    protected final Function<T, ItemStack> iconProvider;
    protected final Function<T, Component> nameProvider;

    public AbstractRegistryEntry(Property<V> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged, Registry<T> registry, Function<T, ItemStack> iconProvider, Function<T, Component> nameProvider) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.registry = registry;
        this.iconProvider = iconProvider;
        this.nameProvider = nameProvider;
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