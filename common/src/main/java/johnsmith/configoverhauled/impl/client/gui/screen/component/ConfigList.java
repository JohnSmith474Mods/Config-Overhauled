package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.AbstractConfigEntry;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.impl.client.gui.entry.CategoryEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class ConfigList extends ContainerObjectSelectionList<AbstractConfigEntry> {
    final Category category;
    final ConfigManager manager;
    private final ConfigScreen configScreen;

    public ConfigList(ConfigScreen screen, Minecraft minecraft, Category category, ConfigManager manager) {
        super(minecraft, 0, 0, 0, 20);
        this.category = category;
        this.manager = manager;
        this.configScreen = screen;

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

                    // Execute widget resolution via the manager's instanced mapper
                    ConfigEntry widget = this.manager.getWidgetMapper().createEntry(
                            property,
                            this.configScreen,
                            this.minecraft,
                            this.configScreen::updateMasterResetButton
                    );

                    this.addEntry((AbstractConfigEntry) widget);
                }
            }
        }
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    @Override
    public void updateSizeAndPosition(int width, int height, int top) {
        super.updateSizeAndPosition(width, height, top);
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() - 3;
    }
}