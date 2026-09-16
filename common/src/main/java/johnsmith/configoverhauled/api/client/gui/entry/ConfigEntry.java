package johnsmith.configoverhauled.api.client.gui.entry;

import net.minecraft.client.gui.components.events.GuiEventListener;

public interface ConfigEntry extends GuiEventListener {
    /** Evaluates if the widget matches the current search bar query. */
    boolean matchesSearch(String query);

    /** Restores the bound property to its defaultValue() and updates the UI state. */
    void resetToDefault();

    /** Evaluates if the current UI state differs from the property's defaultValue(). */
    boolean isModified();
}