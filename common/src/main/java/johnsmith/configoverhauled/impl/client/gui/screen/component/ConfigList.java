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
import johnsmith.configoverhauled.impl.client.gui.entry.OptionEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigList extends ContainerObjectSelectionList<AbstractConfigEntry> {
    final Category category;
    final ConfigManager manager;
    private final ConfigScreen configScreen;

    private final Set<Group> collapsedGroups = new HashSet<>();
    private final Set<Group> searchCollapsedGroups = new HashSet<>();
    private final List<Group> visibleGroups = new ArrayList<>();
    private final List<ConfigEntry> activeMatchingEntries = new ArrayList<>();

    private String currentQuery = "";
    private ConfigSearchQuery parsedQuery = ConfigSearchQuery.EMPTY;

    public ConfigList(ConfigScreen screen, Minecraft minecraft, Category category, ConfigManager manager) {
        super(minecraft, 0, 0, 0, 20);
        this.category = category;
        this.manager = manager;
        this.configScreen = screen;

        this.rebuildEntries(true, null);
    }

    public Category getCategory() {
        return this.category;
    }

    public ConfigManager getManager() {
        return this.manager;
    }

    public void filter(String query) {
        String normalized = query == null ? "" : query;
        boolean queryChanged = !normalized.equals(this.currentQuery);
        this.currentQuery = normalized;
        this.parsedQuery = ConfigSearchQuery.parse(normalized);

        if (queryChanged) {
            this.searchCollapsedGroups.clear();
        }

        this.rebuildEntries(queryChanged, null);
    }

    public boolean isGroupCollapsed(Group group) {
        if (!this.parsedQuery.isEmpty()) {
            return this.searchCollapsedGroups.contains(group);
        }
        return this.collapsedGroups.contains(group);
    }

    public void toggleGroup(Group group) {
        Set<Group> targetSet = this.parsedQuery.isEmpty() ? this.collapsedGroups : this.searchCollapsedGroups;
        if (!targetSet.add(group)) {
            targetSet.remove(group);
        }
        this.rebuildEntries(false, group);
    }

    public boolean hasVisibleGroups() {
        return !this.visibleGroups.isEmpty();
    }

    public boolean areAllVisibleGroupsCollapsed() {
        if (this.visibleGroups.isEmpty()) {
            return false;
        }
        for (Group group : this.visibleGroups) {
            if (!this.isGroupCollapsed(group)) {
                return false;
            }
        }
        return true;
    }

    public void setAllVisibleGroupsCollapsed(boolean collapsed) {
        if (this.visibleGroups.isEmpty()) {
            return;
        }
        Set<Group> targetSet = this.parsedQuery.isEmpty() ? this.collapsedGroups : this.searchCollapsedGroups;
        if (collapsed) {
            targetSet.addAll(this.visibleGroups);
        } else {
            targetSet.removeAll(this.visibleGroups);
        }
        this.rebuildEntries(false, null);
    }

    public int countResettableEntries() {
        int count = 0;
        for (ConfigEntry entry : this.activeMatchingEntries) {
            if (entry instanceof OptionEntry<?, ?> optionEntry) {
                if (optionEntry.canReset()) {
                    count++;
                }
            } else if (entry.isModified()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasModifiedEntries() {
        return this.countResettableEntries() > 0;
    }

    public void resetMatchingEntries() {
        for (ConfigEntry entry : this.activeMatchingEntries) {
            entry.resetToDefault();
        }
    }

    private void rebuildEntries(boolean resetScroll, Group groupToFocus) {
        double previousScroll = this.scrollAmount();
        this.clearEntries();
        this.visibleGroups.clear();
        this.activeMatchingEntries.clear();

        boolean isIngame = this.minecraft.player != null;
        CategoryEntry headerToFocus = null;

        List<Group> groups = this.manager.getGroupsIn(this.category);
        if (groups != null) {
            for (Group group : groups) {
                List<Property<?>> properties = this.manager.getPropertiesIn(group);
                if (properties == null || properties.isEmpty()) continue;

                List<Property<?>> matchingProperties = new ArrayList<>();
                for (Property<?> property : properties) {
                    if (property.scope() == ConfigScope.LEVEL && !isIngame) continue;
                    if (this.parsedQuery.matches(group, property)) {
                        matchingProperties.add(property);
                    }
                }

                if (matchingProperties.isEmpty()) continue;

                this.visibleGroups.add(group);
                boolean collapsed = this.isGroupCollapsed(group);
                CategoryEntry header = new CategoryEntry(group, this.minecraft, collapsed, () -> this.toggleGroup(group));
                this.addEntry(header);

                if (group.equals(groupToFocus)) {
                    headerToFocus = header;
                }

                for (Property<?> property : matchingProperties) {
                    ConfigEntry widget = this.configScreen.createEntry(property);
                    this.activeMatchingEntries.add(widget);
                    if (!collapsed) {
                        this.addEntry((AbstractConfigEntry) widget);
                    }
                }
            }
        }

        if (resetScroll) {
            this.setScrollAmount(0);
        } else {
            this.setScrollAmount(previousScroll);
        }

        if (headerToFocus != null) {
            this.setFocused(headerToFocus);
            headerToFocus.focusToggleButton();
        }
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor guiGraphicsExtractor) {
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor guiGraphicsExtractor) {
    }

    @Override
    public void updateSizeAndPosition(int width, int height, int top) {
        this.setSize(width, height);
        this.setY(top);
        this.setScrollAmount(this.scrollAmount());
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @Override
    protected int scrollBarX() { return super.scrollBarX() - 3; }
}