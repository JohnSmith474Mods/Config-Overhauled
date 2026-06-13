package johnsmith.configoverhauled.api.client.gui.screen;

import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public interface ConfigScreen {
    /** Flags the configuration state as modified, triggering network syncs or reload alerts on close. */
    void markLevelConfigModified();

    /** Passes a tooltip to the screen to be rendered at the highest Z-index during the render cycle. */
    void setDeferredTooltip(List<FormattedCharSequence> tooltip);

    /** Forces an evaluation of the global "Reset All" button state. */
    void updateMasterResetButton();
}