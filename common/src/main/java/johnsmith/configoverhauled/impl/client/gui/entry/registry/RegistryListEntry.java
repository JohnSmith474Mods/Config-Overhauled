package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.AbstractRegistryEntry;
import johnsmith.configoverhauled.impl.client.gui.screen.RegistryListSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class RegistryListEntry<T> extends AbstractRegistryEntry<T, List<T>> {
    public RegistryListEntry(Property<List<T>> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged, Registry<T> registry, Function<T, RenderableIcon> iconProvider, Function<T, Component> nameProvider) {
        super(property, parentScreen, minecraft, onValueChanged, registry, iconProvider, nameProvider);
    }

    @Override
    protected void openSelectionScreen() {
        this.minecraft.setScreen(new RegistryListSelectionScreen<>(
                (Screen) this.parentScreen,
                Component.translatable(this.property.translationKey()),
                this.registry,
                this.property.get(),
                this::applySelection,
                this.iconProvider,
                this.nameProvider
        ));
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setMessage(Component.literal(this.property.get().size() + " Elements"));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + this.property.defaultValue().size() + " Elements");
    }
}