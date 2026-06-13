package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ConfigTab implements Tab {
    private final Component title;
    private final ConfigList list;

    public ConfigTab(ConfigScreen screen, Category category, ConfigManager manager) {
        this.title = Component.translatable(category.translationKey());
        this.list = new ConfigList(screen, Minecraft.getInstance(), category, manager);
    }

    public ConfigList getList() {
        return  this.list;
    }

    @Override
    public @NotNull Component getTabTitle() {
        return this.title;
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> consumer) {
        consumer.accept(this.list);
    }

    @Override
    public void doLayout(ScreenRectangle rectangle) {
        this.list.updateSizeAndPosition(rectangle.width(), rectangle.height(), rectangle.top());
        this.list.setX(rectangle.left());
    }

    public boolean isAvailable() {
        boolean isIngame = Minecraft.getInstance().player != null;
        for (Group group : this.list.manager.getGroupsIn(this.list.category)) {
            for (Property<?> property : this.list.manager.getPropertiesIn(group)) {
                if (property.scope() != ConfigScope.LEVEL || isIngame) return true;
            }
        }
        return false;
    }
}