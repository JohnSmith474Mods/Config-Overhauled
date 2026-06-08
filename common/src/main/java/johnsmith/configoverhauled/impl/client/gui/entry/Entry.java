package johnsmith.configoverhauled.impl.client.gui.entry;

import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    public abstract boolean matchesSearch(String query);
}