package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.entry.Entry;
import johnsmith.configoverhauled.impl.client.gui.registry.ConfigWidgetRegistry;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * An abstract foundation for configuration lists that replaces hardcoded
 * instance-type checks with dynamic, registry-driven GUI entry resolution.
 */
public abstract class AbstractConfigList extends ContainerObjectSelectionList<Entry> {
    protected final Runnable onValueChanged;
    protected final AbstractConfigScreen parentScreen;

    public AbstractConfigList(AbstractConfigScreen parentScreen, Minecraft minecraft, int width, int height, int top, int itemHeight, Runnable onValueChanged) {
        super(minecraft, width, height, top, itemHeight);
        this.parentScreen = parentScreen;
        this.onValueChanged = onValueChanged;
    }

    /**
     * Dynamically resolves and appends the appropriate GUI entry for a given Property.
     *
     * @param config The property configuration to inject into the layout.
     */
    protected void addConfigEntry(Property<?> config) {
        this.addEntry(ConfigWidgetRegistry.createEntry(config, this.parentScreen, this.minecraft, this.onValueChanged));
    }
}
