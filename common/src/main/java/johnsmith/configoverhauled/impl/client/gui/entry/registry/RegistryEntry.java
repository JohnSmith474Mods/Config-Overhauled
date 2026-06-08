package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.entry.AbstractRegistryEntry;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.screen.RegistrySelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class RegistryEntry<T> extends AbstractRegistryEntry<T, T> {

    public RegistryEntry(Property<T> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged, Registry<T> registry, Function<T, ItemStack> iconProvider, Function<T, Component> nameProvider) {
        super(property, parentScreen, minecraft, onValueChanged, registry, iconProvider, nameProvider);
        this.updateWidgetValue();
    }

    @Override
    protected void openSelectionScreen() {
        this.minecraft.setScreen(new RegistrySelectionScreen<>(
                this.parentScreen,
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
        this.widget.setMessage(this.nameProvider.apply(this.property.get()));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: ").append(this.nameProvider.apply(this.property.defaultValue()));
    }


}