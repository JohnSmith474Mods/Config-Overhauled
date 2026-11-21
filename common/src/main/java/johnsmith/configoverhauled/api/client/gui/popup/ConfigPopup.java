package johnsmith.configoverhauled.api.client.gui.popup;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Contract for an overlay modal popup hosted inside a {@link johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen}.
 * <p>
 * Popups render on a dedicated stratum above the parent screen without replacing the active {@code Screen},
 * preserving tab selection, scroll offsets, search queries, and group collapse states.
 */
public interface ConfigPopup extends GuiEventListener {

    /**
     * Initializes or recalculates the popup's layout for the given parent screen dimensions.
     * Invoked when the popup is first opened and whenever the window is resized.
     *
     * @param screenWidth  The current width of the parent screen.
     * @param screenHeight The current height of the parent screen.
     */
    void init(int screenWidth, int screenHeight);

    /**
     * Renders the popup backdrop, frame, and child widgets on top of the parent screen.
     *
     * @param guiGraphics The graphics extractor for the current frame.
     * @param mouseX      The current mouse X coordinate.
     * @param mouseY      The current mouse Y coordinate.
     * @param partialTick The partial frame tick delta.
     */
    void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick);

    /**
     * Optional per-tick update hook for animations or timed UI states.
     */
    default void tick() {
    }

    /**
     * Invoked when the popup is dismissed, replaced, or when the parent screen closes.
     * Implementations that allocate GPU resources (such as dynamic textures) should release them here.
     */
    default void onClose() {
    }

    /**
     * Whether left-clicking outside the popup frame should dismiss it.
     *
     * @return {@code true} to close when clicking outside the modal bounds.
     */
    default boolean closeOnOutsideClick() {
        return true;
    }

    /**
     * Whether pressing the Escape key should dismiss the popup.
     *
     * @return {@code true} to close on Escape.
     */
    default boolean closeOnEscape() {
        return true;
    }

    @Override
    default void setFocused(boolean focused) {
    }

    @Override
    default boolean isFocused() {
        return true;
    }
}
