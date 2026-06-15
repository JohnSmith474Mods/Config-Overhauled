package johnsmith.configoverhauled.api.client.gui.entry;

import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * The base abstract implementation for all configuration UI entries.
 * <p>
 * This class bridges the configuration API with Minecraft's native {@link ContainerObjectSelectionList.Entry},
 * allowing configuration properties to be rendered and interacted with as rows within a scrolling list.
 */
public abstract class AbstractConfigEntry extends ContainerObjectSelectionList.Entry<AbstractConfigEntry> {
}