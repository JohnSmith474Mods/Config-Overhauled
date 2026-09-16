package johnsmith.configoverhauled.impl.core.state;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;

public record DefaultCategory(String id, ConfigManager manager) implements Category {
    @Override
    public String translationKey() {
        return manager.modId() + ".config." + id;
    }

    @Override
    public Group define(String id) {
        return manager.registerGroup(this, id);
    }

    @Override
    public void remove(Group group) {
        this.manager().remove(group);
        if (this.isEmpty()) this.manager().remove(this);
    }

    @Override
    public boolean isEmpty() {
        return this.manager().getGroupsIn(this).isEmpty();
    }
}