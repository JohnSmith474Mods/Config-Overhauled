package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.impl.client.gui.entry.CategoryEntry;
import johnsmith.configoverhauled.impl.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * A custom implementation of {@link ContainerObjectSelectionList} used to render
 * the list of configuration categories and adjustable properties for a single {@link ConfigTab}.
 * <p>
 * This list dynamically populates itself based on the structure defined in the {@link ConfigManager},
 * grouping individual properties under category headers. It overrides rendering methods to
 * provide a transparent background suitable for overlaying other GUI elements.
 */
public class ConfigList extends AbstractConfigList {
    final Category category;
    final ConfigManager manager;

    /**
     * Constructs a new configuration list.
     *
     * @param screen      The parent configuration screen, used for accessing layout dimensions and callbacks.
     * @param minecraft   The Minecraft client instance.
     * @param category    The category containing the groups and properties to display.
     * @param manager     The central configuration manager instance.
     */
    public ConfigList(ConfigScreen screen, Minecraft minecraft, Category category, ConfigManager manager) {
        super(screen, minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 20, screen::updateMasterResetButton);
        this.category = category;
        this.manager = manager;

        this.filter("");
    }

    public void filter(String query) {
        this.clearEntries();
        this.setScrollAmount(0);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        boolean isIngame = this.minecraft.player != null;

        for (Group group : this.manager.getGroupsIn(this.category)) {
            boolean groupAdded = false;

            for (Property<?> property : this.manager.getPropertiesIn(group)) {
                if (property.scope() == ConfigScope.LEVEL && !isIngame) continue;

                String translatedName = Component.translatable(property.translationKey()).getString().toLowerCase(Locale.ROOT);

                if (lowerQuery.isEmpty() || translatedName.contains(lowerQuery)) {
                    if (!groupAdded) {
                        this.addEntry(new CategoryEntry(group, this.minecraft));
                        groupAdded = true;
                    }

                    this.addConfigEntry(property);
                }
            }
        }
    }

    /**
     * Overridden to prevent rendering a background texture, making the list transparent.
     *
     * @param guiGraphics The graphics context for rendering.
     */
    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    /**
     * Overridden to prevent rendering list separators (shadows/outlines).
     *
     * @param guiGraphics The graphics context for rendering.
     */
    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSizeAndPosition(int width, int height, int top) {
        this.setSize(width, height);
        this.setY(top);
        this.setScrollAmount(this.scrollAmount());
    }

    /**
     * Specifies a fixed width for the content rows within the list.
     *
     * @return The width of the content area (340 pixels).
     */
    @Override
    public int getRowWidth() { return 340; }

    /**
     * Adjusts the calculated scrollbar position to align it visually.
     *
     * @return The adjusted scrollbar x-coordinate.
     */
    @Override
    protected int scrollBarX() { return super.scrollBarX() - 3; }
}