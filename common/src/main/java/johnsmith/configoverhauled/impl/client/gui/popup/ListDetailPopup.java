package johnsmith.configoverhauled.impl.client.gui.popup;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.popup.AbstractConfigPopup;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Structured modal list editor for {@link Property}{@code <List<E>>} entries.
 * <p>
 * Supports add, remove, reorder (up/down), scrollable scissor-clipped row editing with
 * per-element {@link Codec} validation, a plain-string fast path for {@link Codec#STRING},
 * and a lossless toggle to a Raw JSON array editor.
 *
 * @param <E> The list element type.
 */
public class ListDetailPopup<E> extends AbstractConfigPopup<List<E>> {
    private static final int TOOLBAR_HEIGHT = 18;
    private static final int TOOLBAR_GAP = 5;
    private static final int STATUS_HEIGHT = 12;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDGET_HEIGHT = 18;
    private static final int MIN_INDEX_GUTTER_WIDTH = 22;
    private static final int ACTION_BUTTON_WIDTH = 16;
    private static final int ACTION_BUTTON_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 5;

    private static final int VIEWPORT_BG = 0xFF0E0E0E;
    private static final int VIEWPORT_BORDER = 0xFF555555;
    private static final int SCROLLBAR_TRACK = 0xFF222222;
    private static final int SCROLLBAR_THUMB = 0xFF9A9A9A;
    private static final int SCROLLBAR_THUMB_ACTIVE = 0xFFD0D0D0;
    private static final int VALID_TEXT_COLOR = 0xFFFFFFFF;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int MUTED_TEXT_COLOR = 0xFFAAAAAA;
    private static final int ARROW_ACTIVE_COLOR = 0xFFE6E6E6;
    private static final int ARROW_DISABLED_COLOR = 0xFF666666;

    private final Property<List<E>> listProperty;
    private final @Nullable Codec<E> elementCodec;
    private final boolean structuredSupported;
    private final boolean plainStringMode;

    // Persistent working state across window resizes and mode switches
    private final List<RowState<E>> rows = new ArrayList<>();
    private boolean rawJsonMode;
    private String rawJsonText;
    private @Nullable List<E> rawParsedList;
    private @Nullable Component rawError;

    // Scroll state (structured mode)
    private double scrollOffset = 0.0;
    private boolean draggingScrollbar = false;
    private double scrollbarDragGrabY = 0.0;
    private boolean draggingRowWidget = false;

    // Layout bounds computed in initContent / rebuildRowWidgets
    private int toolbarY;
    private int viewportLeft;
    private int viewportTop;
    private int viewportWidth;
    private int viewportHeight;
    private int statusY;
    private int indexGutterWidth = MIN_INDEX_GUTTER_WIDTH;

    // Toolbar / raw mode widgets (registered with AbstractConfigPopup)
    private @Nullable Button addButton;
    private @Nullable Button modeToggleButton;
    private @Nullable EditBox rawJsonBox;
    private @Nullable Button rawClearButton;

    // Structured row widgets (managed and scissor-clipped locally)
    private final List<RowWidgets> rowWidgets = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ListDetailPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Property<List<E>> property,
            String initialJsonText,
            boolean isLocked,
            Consumer<List<E>> onApply
    ) {
        super(parentScreen, minecraft, property, isLocked, onApply);
        this.listProperty = property;

        if (property instanceof DefaultProperty.List<?> defaultList) {
            this.elementCodec = (Codec<E>) defaultList.elementCodec();
            this.structuredSupported = true;
            this.plainStringMode = this.elementCodec == Codec.STRING;
        } else {
            this.elementCodec = null;
            this.structuredSupported = false;
            this.plainStringMode = false;
        }

        String candidateJson = initialJsonText != null ? initialJsonText : this.encodeListToJson(property.get());
        List<E> parsedInitial = this.tryParseJsonList(candidateJson);

        if (this.structuredSupported && parsedInitial != null) {
            this.rawJsonMode = false;
            this.loadRowsFromList(parsedInitial);
            this.rawJsonText = candidateJson;
            this.rawParsedList = new ArrayList<>(parsedInitial);
            this.rawError = null;
        } else {
            // Preserve invalid uncommitted inline JSON in Raw JSON mode, or handle custom non-DefaultProperty.List
            this.rawJsonMode = true;
            this.rawJsonText = candidateJson;
            this.validateRawJson(this.rawJsonText);
            if (this.structuredSupported) {
                this.loadRowsFromList(property.get());
            }
        }
    }

    @Override
    protected int getPreferredWidth() {
        return 370;
    }

    @Override
    protected int getPreferredHeight() {
        return 248;
    }

    @Override
    protected Component getDefaultValueTooltip() {
        String encoded = this.encodeListToJson(this.listProperty.defaultValue());
        return Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": " + encoded));
    }

    @Override
    protected void initContent(int left, int top, int width, int height) {
        this.toolbarY = top;
        this.viewportLeft = left;
        this.viewportTop = top + TOOLBAR_HEIGHT + TOOLBAR_GAP;
        this.viewportWidth = width;
        this.viewportHeight = Math.max(48, height - TOOLBAR_HEIGHT - TOOLBAR_GAP - STATUS_HEIGHT);
        this.statusY = this.viewportTop + this.viewportHeight + 3;

        this.rowWidgets.clear();
        this.addButton = null;
        this.modeToggleButton = null;
        this.rawJsonBox = null;
        this.rawClearButton = null;

        int rightCursor = left + width;
        if (this.structuredSupported) {
            int toggleWidth = 72;
            rightCursor -= toggleWidth;
            Component toggleLabel = Component.translatable(
                    this.rawJsonMode
                            ? "config_overhauled.popup.list.structured"
                            : "config_overhauled.popup.list.raw_json"
            );
            this.modeToggleButton = this.addRenderableWidget(
                    Button.builder(toggleLabel, b -> this.toggleMode())
                            .bounds(rightCursor, this.toolbarY, toggleWidth, TOOLBAR_HEIGHT)
                            .build()
            );

            if (!this.rawJsonMode) {
                int addWidth = 52;
                rightCursor -= (4 + addWidth);
                this.addButton = this.addRenderableWidget(
                        Button.builder(Component.translatable("config_overhauled.popup.list.add"), b -> this.addRow())
                                .bounds(rightCursor, this.toolbarY, addWidth, TOOLBAR_HEIGHT)
                                .build()
                );
                this.addButton.active = !this.isLocked;
            }
        }

        if (this.rawJsonMode) {
            this.initRawJsonWidgets(left, this.viewportTop + 4, width);
        } else {
            this.rebuildRowWidgets(-1);
        }

        this.updateToolbarStates();
        this.updateFooterButtons();
    }

    private void initRawJsonWidgets(int left, int boxY, int width) {
        int clearSize = 20;
        int clearGap = 4;
        int boxWidth = Math.max(80, width - clearSize - clearGap);

        this.rawJsonBox = new EditBox(this.font, left, boxY, boxWidth, 20, this.title) {
            @Override
            public boolean keyPressed(KeyEvent event) {
                if ((event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)
                        && !ListDetailPopup.this.isLocked
                        && ListDetailPopup.this.isWorkingValueValid()) {
                    ListDetailPopup.this.applyAndClose();
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        this.rawJsonBox.setMaxLength(32767);
        this.rawJsonBox.setValue(this.rawJsonText);
        this.rawJsonBox.setEditable(!this.isLocked);
        this.rawJsonBox.setResponder(text -> {
            this.rawJsonText = text;
            this.validateRawJson(text);
            this.updateRawBoxColor();
            this.updateToolbarStates();
            this.updateFooterButtons();
        });
        this.addRenderableWidget(this.rawJsonBox);

        this.rawClearButton = this.addRenderableWidget(
                Button.builder(Component.literal("X"), b -> {
                    if (!this.isLocked && this.rawJsonBox != null) {
                        this.rawJsonBox.setValue("[]");
                        this.setFocusedWidget(this.rawJsonBox);
                    }
                }).bounds(left + boxWidth + clearGap, boxY, clearSize, 20).build()
        );
        this.rawClearButton.active = !this.isLocked;

        this.validateRawJson(this.rawJsonText);
        this.updateRawBoxColor();
        this.setFocusedWidget(this.rawJsonBox);
    }

    private void updateRawBoxColor() {
        if (this.rawJsonBox != null) {
            this.rawJsonBox.setTextColor(this.rawParsedList != null ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
        }
    }

    private void rebuildRowWidgets(int focusRowIndex) {
        // Clear focus if it belonged to an old row widget
        GuiEventListener currentFocus = this.getFocusedWidget();
        for (RowWidgets rw : this.rowWidgets) {
            if (currentFocus == rw.editBox || currentFocus == rw.upButton || currentFocus == rw.downButton || currentFocus == rw.removeButton) {
                this.setFocusedWidget(null);
                break;
            }
        }
        this.rowWidgets.clear();

        int maxScroll = this.getMaxScroll();
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, maxScroll);

        // Always reserve the scrollbar lane so the side buttons never shift horizontally when scrolling activates.
        int rightEdge = this.viewportLeft + this.viewportWidth - 4 - (SCROLLBAR_WIDTH + 3);
        int removeX = rightEdge - ACTION_BUTTON_WIDTH;
        int downX = removeX - ACTION_BUTTON_GAP - ACTION_BUTTON_WIDTH;
        int upX = downX - ACTION_BUTTON_GAP - ACTION_BUTTON_WIDTH;

        int maxIdxWidth = this.font.width(Math.max(1, this.rows.size()) + ".");
        this.indexGutterWidth = Math.max(MIN_INDEX_GUTTER_WIDTH, maxIdxWidth);
        int editLeft = this.viewportLeft + 4 + this.indexGutterWidth + 4;
        int editWidth = Math.max(60, upX - 4 - editLeft);

        for (int i = 0; i < this.rows.size(); i++) {
            final int rowIndex = i;
            RowState<E> state = this.rows.get(i);

            EditBox editBox = new EditBox(this.font, editLeft, 0, editWidth, ROW_WIDGET_HEIGHT, Component.literal("#" + (i + 1))) {
                @Override
                public boolean keyPressed(KeyEvent event) {
                    if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                        boolean isCtrl = event.hasControlDownWithQuirk() || event.hasControlDown();
                        if (isCtrl) {
                            if (!ListDetailPopup.this.isLocked && ListDetailPopup.this.isWorkingValueValid()) {
                                ListDetailPopup.this.applyAndClose();
                            }
                            return true;
                        }
                        if (rowIndex + 1 < ListDetailPopup.this.rowWidgets.size()) {
                            ListDetailPopup.this.focusRowEditBox(rowIndex + 1);
                            return true;
                        }
                        return true;
                    }
                    return super.keyPressed(event);
                }
            };
            editBox.setMaxLength(32767);
            editBox.setValue(state.text);
            editBox.setCursorPosition(0);
            editBox.setHighlightPos(0);
            editBox.setEditable(!this.isLocked);
            editBox.setTextColor(state.valid ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
            editBox.setResponder(text -> {
                state.text = text;
                this.validateRow(state);
                editBox.setTextColor(state.valid ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
                this.updateToolbarStates();
                this.updateFooterButtons();
            });

            Button upButton = Button.builder(Component.empty(), b -> this.moveRow(rowIndex, -1))
                    .bounds(upX, 0, ACTION_BUTTON_WIDTH, ROW_WIDGET_HEIGHT)
                    .createNarration(s -> Component.translatable("config_overhauled.popup.list.move_up"))
                    .build();
            upButton.active = !this.isLocked && i > 0;

            Button downButton = Button.builder(Component.empty(), b -> this.moveRow(rowIndex, 1))
                    .bounds(downX, 0, ACTION_BUTTON_WIDTH, ROW_WIDGET_HEIGHT)
                    .createNarration(s -> Component.translatable("config_overhauled.popup.list.move_down"))
                    .build();
            downButton.active = !this.isLocked && i < this.rows.size() - 1;

            Button removeButton = Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> this.removeRow(rowIndex))
                    .bounds(removeX, 0, ACTION_BUTTON_WIDTH, ROW_WIDGET_HEIGHT)
                    .createNarration(s -> Component.translatable("config_overhauled.popup.list.remove"))
                    .build();
            removeButton.active = !this.isLocked;

            this.rowWidgets.add(new RowWidgets(editBox, upButton, downButton, removeButton));
        }

        this.repositionRowWidgets();

        if (focusRowIndex >= 0 && focusRowIndex < this.rowWidgets.size()) {
            this.focusRowEditBox(focusRowIndex);
            EditBox focusedBox = this.rowWidgets.get(focusRowIndex).editBox;
            focusedBox.moveCursorToEnd(false);
            focusedBox.setHighlightPos(0);
        }
    }

    private void repositionRowWidgets() {
        int topOffset = this.viewportTop + 3 - (int) Math.round(this.scrollOffset);
        int widgetYOffset = (ROW_HEIGHT - ROW_WIDGET_HEIGHT) / 2;
        for (int i = 0; i < this.rowWidgets.size(); i++) {
            int rowY = topOffset + i * ROW_HEIGHT;
            int widgetY = rowY + widgetYOffset;
            RowWidgets rw = this.rowWidgets.get(i);
            rw.editBox.setY(widgetY);
            rw.upButton.setY(widgetY);
            rw.downButton.setY(widgetY);
            rw.removeButton.setY(widgetY);
        }
    }

    private void focusRowEditBox(int index) {
        if (index < 0 || index >= this.rowWidgets.size()) {
            return;
        }
        this.ensureRowVisible(index);
        this.setFocusedWidget(this.rowWidgets.get(index).editBox);
    }

    private void ensureRowVisible(int index) {
        int innerHeight = Math.max(1, this.viewportHeight - 2);
        int rowTop = 2 + index * ROW_HEIGHT;
        int rowBottom = rowTop + ROW_HEIGHT;
        if (rowTop < this.scrollOffset) {
            this.scrollOffset = rowTop;
        } else if (rowBottom > this.scrollOffset + innerHeight) {
            this.scrollOffset = rowBottom - innerHeight;
        }
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, this.getMaxScroll());
        this.repositionRowWidgets();
    }

    private void addRow() {
        if (this.isLocked || this.rawJsonMode) {
            return;
        }
        String templateText = "";
        if (!this.plainStringMode && !this.listProperty.defaultValue().isEmpty()) {
            templateText = this.encodeElement(this.listProperty.defaultValue().getFirst());
        }
        RowState<E> state = new RowState<>(templateText);
        this.validateRow(state);
        this.rows.add(state);

        int newIndex = this.rows.size() - 1;
        this.scrollOffset = this.getMaxScroll();
        this.rebuildRowWidgets(newIndex);
        this.updateToolbarStates();
        this.updateFooterButtons();
    }

    private void moveRow(int index, int delta) {
        if (this.isLocked || this.rawJsonMode) {
            return;
        }
        int target = index + delta;
        if (index < 0 || index >= this.rows.size() || target < 0 || target >= this.rows.size()) {
            return;
        }
        RowState<E> moved = this.rows.remove(index);
        this.rows.add(target, moved);
        this.rebuildRowWidgets(-1);
        this.ensureRowVisible(target);
        this.updateToolbarStates();
        this.updateFooterButtons();
    }

    private void removeRow(int index) {
        if (this.isLocked || this.rawJsonMode) {
            return;
        }
        if (index < 0 || index >= this.rows.size()) {
            return;
        }
        this.rows.remove(index);
        this.rebuildRowWidgets(-1);
        this.updateToolbarStates();
        this.updateFooterButtons();
    }

    private void toggleMode() {
        if (!this.structuredSupported) {
            return;
        }
        if (this.rawJsonMode) {
            if (this.rawParsedList == null) {
                return;
            }
            this.loadRowsFromList(this.rawParsedList);
            this.scrollOffset = 0.0;
            this.rawJsonMode = false;
        } else {
            if (!this.areAllRowsValid()) {
                return;
            }
            List<E> currentList = this.buildStructuredList();
            this.rawJsonText = this.encodeListToJson(currentList);
            this.rawParsedList = currentList;
            this.rawError = null;
            this.rawJsonMode = true;
        }
        this.init(this.screenWidth, this.screenHeight);
    }

    private void updateToolbarStates() {
        if (this.modeToggleButton != null) {
            this.modeToggleButton.active = this.rawJsonMode
                    ? this.rawParsedList != null
                    : this.areAllRowsValid();
        }
        if (this.addButton != null) {
            this.addButton.active = !this.isLocked;
        }
    }

    private void loadRowsFromList(List<E> source) {
        this.rows.clear();
        for (E element : source) {
            RowState<E> state = new RowState<>(this.encodeElement(element));
            this.validateRow(state);
            this.rows.add(state);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateRow(RowState<E> state) {
        if (this.plainStringMode) {
            state.parsedValue = (E) state.text;
            state.valid = true;
            state.errorMessage = null;
            return;
        }
        if (this.elementCodec == null) {
            state.parsedValue = null;
            state.valid = false;
            state.errorMessage = "Unsupported element codec";
            return;
        }

        String raw = state.text.trim();
        if (raw.isEmpty()) {
            DataResult<E> emptyStrResult = this.elementCodec.parse(JsonOps.INSTANCE, new JsonPrimitive(""));
            if (emptyStrResult.result().isPresent()) {
                state.parsedValue = emptyStrResult.result().get();
                state.valid = true;
                state.errorMessage = null;
            } else {
                state.parsedValue = null;
                state.valid = false;
                state.errorMessage = emptyStrResult.error().map(DataResult.Error::message).orElse("Empty value");
            }
            return;
        }

        String primaryError = null;
        try {
            JsonElement json = JsonParser.parseString(raw);
            DataResult<E> parsed = this.elementCodec.parse(JsonOps.INSTANCE, json);
            if (parsed.result().isPresent()) {
                state.parsedValue = parsed.result().get();
                state.valid = true;
                state.errorMessage = null;
                return;
            }
            primaryError = parsed.error().map(DataResult.Error::message).orElse(null);
        } catch (Exception e) {
            primaryError = e.getMessage();
        }

        // Fallback: allow unquoted string-backed codec values (e.g. identifiers, enums)
        if (!raw.startsWith("\"") && !raw.startsWith("{") && !raw.startsWith("[")) {
            DataResult<E> stringFallback = this.elementCodec.parse(JsonOps.INSTANCE, new JsonPrimitive(state.text));
            if (stringFallback.result().isPresent()) {
                state.parsedValue = stringFallback.result().get();
                state.valid = true;
                state.errorMessage = null;
                return;
            }
        }

        state.parsedValue = null;
        state.valid = false;
        state.errorMessage = primaryError != null && !primaryError.isBlank()
                ? primaryError
                : Component.translatable("config_overhauled.popup.list.invalid_element").getString();
    }

    private String encodeElement(E element) {
        if (this.plainStringMode) {
            return element != null ? element.toString() : "";
        }
        if (this.elementCodec == null) {
            return String.valueOf(element);
        }
        return this.elementCodec.encodeStart(JsonOps.INSTANCE, element)
                .result()
                .map(json -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()
                        ? json.getAsString()
                        : json.toString())
                .orElseGet(() -> String.valueOf(element));
    }

    private String encodeListToJson(List<E> list) {
        return this.listProperty.codec().encodeStart(JsonOps.INSTANCE, list)
                .result()
                .map(JsonElement::toString)
                .orElse("[]");
    }

    private @Nullable List<E> tryParseJsonList(String jsonText) {
        try {
            JsonElement element = JsonParser.parseString(jsonText);
            return this.listProperty.codec().parse(JsonOps.INSTANCE, element).result().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateRawJson(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            DataResult<List<E>> result = this.listProperty.codec().parse(JsonOps.INSTANCE, element);
            if (result.result().isPresent()) {
                this.rawParsedList = new ArrayList<>(result.result().get());
                this.rawError = null;
            } else {
                this.rawParsedList = null;
                this.rawError = Component.translatable("config_overhauled.popup.list.invalid_json");
            }
        } catch (Exception e) {
            this.rawParsedList = null;
            this.rawError = Component.translatable("config_overhauled.popup.list.invalid_json");
        }
    }

    private boolean areAllRowsValid() {
        for (RowState<E> row : this.rows) {
            if (!row.valid) {
                return false;
            }
        }
        return true;
    }

    private int countInvalidRows() {
        int count = 0;
        for (RowState<E> row : this.rows) {
            if (!row.valid) {
                count++;
            }
        }
        return count;
    }

    private List<E> buildStructuredList() {
        List<E> result = new ArrayList<>(this.rows.size());
        for (RowState<E> row : this.rows) {
            result.add(row.parsedValue);
        }
        return result;
    }

    private int getContentTotalHeight() {
        return this.rows.size() * ROW_HEIGHT + 4;
    }

    private int getMaxScroll() {
        int innerHeight = Math.max(1, this.viewportHeight - 2);
        return Math.max(0, this.getContentTotalHeight() - innerHeight);
    }

    @Override
    protected boolean isWorkingValueValid() {
        return this.rawJsonMode ? this.rawParsedList != null : this.areAllRowsValid();
    }

    @Override
    protected boolean canResetWorkingValue() {
        if (!this.isWorkingValueValid()) {
            return true;
        }
        return !Objects.equals(this.buildResultValue(), this.listProperty.defaultValue());
    }

    @Override
    protected void resetWorkingValue() {
        List<E> defaultList = this.listProperty.defaultValue();
        this.rawJsonText = this.encodeListToJson(defaultList);
        this.rawParsedList = new ArrayList<>(defaultList);
        this.rawError = null;
        if (this.structuredSupported) {
            this.loadRowsFromList(defaultList);
        }
        this.scrollOffset = 0.0;
        if (this.rawJsonMode && this.rawJsonBox != null) {
            this.rawJsonBox.setValue(this.rawJsonText);
            this.updateRawBoxColor();
        } else {
            this.rebuildRowWidgets(-1);
        }
        this.updateToolbarStates();
        this.updateFooterButtons();
    }

    @Override
    protected List<E> buildResultValue() {
        return this.rawJsonMode
                ? (this.rawParsedList != null ? List.copyOf(this.rawParsedList) : this.listProperty.get())
                : List.copyOf(this.buildStructuredList());
    }

    @Override
    protected void extractBody(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Toolbar left counter label
        int itemCount = this.rawJsonMode
                ? (this.rawParsedList != null ? this.rawParsedList.size() : this.rows.size())
                : this.rows.size();
        Component countLabel = Component.translatable("config_overhauled.popup.list.items_count", itemCount);
        int labelY = this.toolbarY + (TOOLBAR_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.text(this.font, countLabel, this.viewportLeft, labelY, 0xFFDDDDDD, false);

        if (this.rawJsonMode) {
            this.extractRawJsonBody(guiGraphics);
        } else {
            this.extractStructuredViewport(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Mode toggle disabled tooltip hint
        if (this.modeToggleButton != null && !this.modeToggleButton.active && this.modeToggleButton.isMouseOver(mouseX, mouseY)) {
            this.parentScreen.setDeferredTooltip(List.of(
                    Component.translatable("config_overhauled.popup.list.fix_errors_to_switch").getVisualOrderText()
            ));
        }
    }

    private void extractRawJsonBody(GuiGraphicsExtractor guiGraphics) {
        int infoTop = this.viewportTop + 28;
        if (this.rawError != null) {
            String trimmed = this.font.plainSubstrByWidth(this.rawError.getString(), this.viewportWidth);
            guiGraphics.text(this.font, trimmed, this.viewportLeft, infoTop, INVALID_TEXT_COLOR, false);
            infoTop += this.font.lineHeight + 4;
        }
        String defaultText = Component.translatable("config_overhauled.word.default").getString()
                + ": " + this.encodeListToJson(this.listProperty.defaultValue());
        String trimmedDefault = this.font.plainSubstrByWidth(defaultText, this.viewportWidth);
        guiGraphics.text(this.font, trimmedDefault, this.viewportLeft, infoTop, MUTED_TEXT_COLOR, false);
    }

    private void extractStructuredViewport(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int vpRight = this.viewportLeft + this.viewportWidth;
        int vpBottom = this.viewportTop + this.viewportHeight;

        guiGraphics.fill(this.viewportLeft, this.viewportTop, vpRight, vpBottom, VIEWPORT_BG);
        guiGraphics.outline(this.viewportLeft, this.viewportTop, this.viewportWidth, this.viewportHeight, VIEWPORT_BORDER);

        if (this.rows.isEmpty()) {
            Component emptyText = Component.translatable(
                    this.isLocked ? "config_overhauled.popup.list.empty_locked" : "config_overhauled.popup.list.empty"
            );
            int centerX = this.viewportLeft + this.viewportWidth / 2;
            int centerY = this.viewportTop + (this.viewportHeight - this.font.lineHeight) / 2;
            guiGraphics.centeredText(this.font, emptyText, centerX, centerY, MUTED_TEXT_COLOR);
            return;
        }

        int maxScroll = this.getMaxScroll();
        boolean hasScrollbar = maxScroll > 0;
        int clipRight = vpRight - 1 - (SCROLLBAR_WIDTH + 2);

        boolean cursorInViewport = this.isInsideViewportContent(mouseX, mouseY);
        int effectiveMouseX = cursorInViewport ? mouseX : -1;
        int effectiveMouseY = cursorInViewport ? mouseY : -1;

        List<FormattedCharSequence> hoveredRowTooltip = null;
        int firstVisible = this.getFirstVisibleRowIndex();
        int lastVisible = this.getLastVisibleRowIndex();

        guiGraphics.enableScissor(this.viewportLeft + 1, this.viewportTop + 1, clipRight, vpBottom - 1);
        for (int i = firstVisible; i <= lastVisible; i++) {
            RowWidgets rw = this.rowWidgets.get(i);
            int widgetTop = rw.editBox.getY();
            int widgetBottom = widgetTop + ROW_WIDGET_HEIGHT;
            if (widgetBottom < this.viewportTop || widgetTop > vpBottom) {
                continue;
            }

            // Right-aligned 1-based row index
            String idxStr = (i + 1) + ".";
            int idxW = this.font.width(idxStr);
            int idxX = this.viewportLeft + 4 + this.indexGutterWidth - idxW;
            int idxY = widgetTop + (ROW_WIDGET_HEIGHT - this.font.lineHeight) / 2;
            guiGraphics.text(this.font, idxStr, idxX, idxY, MUTED_TEXT_COLOR, false);

            rw.editBox.extractRenderState(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);
            rw.upButton.extractRenderState(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);
            drawArrowIcon(guiGraphics, rw.upButton, true);
            rw.downButton.extractRenderState(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);
            drawArrowIcon(guiGraphics, rw.downButton, false);
            rw.removeButton.extractRenderState(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);

            if (cursorInViewport) {
                RowState<E> state = this.rows.get(i);
                if (!state.valid && state.errorMessage != null && rw.editBox.isMouseOver(mouseX, mouseY)) {
                    hoveredRowTooltip = this.font.split(Component.literal(state.errorMessage).withStyle(ChatFormatting.RED), 220);
                } else if (rw.upButton.isMouseOver(mouseX, mouseY) && rw.upButton.active) {
                    hoveredRowTooltip = List.of(Component.translatable("config_overhauled.popup.list.move_up").getVisualOrderText());
                } else if (rw.downButton.isMouseOver(mouseX, mouseY) && rw.downButton.active) {
                    hoveredRowTooltip = List.of(Component.translatable("config_overhauled.popup.list.move_down").getVisualOrderText());
                } else if (rw.removeButton.isMouseOver(mouseX, mouseY) && rw.removeButton.active) {
                    hoveredRowTooltip = List.of(Component.translatable("config_overhauled.popup.list.remove").getVisualOrderText());
                }
            }
        }
        guiGraphics.disableScissor();

        if (hoveredRowTooltip != null) {
            this.parentScreen.setDeferredTooltip(hoveredRowTooltip);
        }

        if (hasScrollbar) {
            this.extractScrollbar(guiGraphics, mouseX, mouseY, maxScroll);
        }

        // Status / error strip beneath the viewport
        int invalidCount = this.countInvalidRows();
        if (invalidCount > 0) {
            Component err = Component.translatable("config_overhauled.popup.list.invalid_rows", invalidCount);
            guiGraphics.text(this.font, err, this.viewportLeft, this.statusY, INVALID_TEXT_COLOR, false);
        }
    }

    private void extractScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int maxScroll) {
        int trackLeft = this.getScrollbarLeft();
        int trackTop = this.viewportTop + 2;
        int trackHeight = Math.max(12, this.viewportHeight - 4);
        guiGraphics.fill(trackLeft, trackTop, trackLeft + SCROLLBAR_WIDTH, trackTop + trackHeight, SCROLLBAR_TRACK);

        int thumbHeight = this.getScrollbarThumbHeight(trackHeight);
        int thumbTop = trackTop + this.getScrollbarThumbOffset(trackHeight, thumbHeight, maxScroll);
        boolean active = this.draggingScrollbar
                || (mouseX >= trackLeft && mouseX < trackLeft + SCROLLBAR_WIDTH && mouseY >= thumbTop && mouseY < thumbTop + thumbHeight);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight,
                active ? SCROLLBAR_THUMB_ACTIVE : SCROLLBAR_THUMB);
    }

    private static void drawArrowIcon(GuiGraphicsExtractor guiGraphics, Button button, boolean up) {
        int cx = button.getX() + button.getWidth() / 2;
        int cy = button.getY() + button.getHeight() / 2;
        int color = button.active ? ARROW_ACTIVE_COLOR : ARROW_DISABLED_COLOR;
        if (up) {
            guiGraphics.fill(cx, cy - 2, cx + 1, cy - 1, color);
            guiGraphics.fill(cx - 1, cy - 1, cx + 2, cy, color);
            guiGraphics.fill(cx - 2, cy, cx + 3, cy + 1, color);
            guiGraphics.fill(cx - 3, cy + 1, cx + 4, cy + 2, color);
        } else {
            guiGraphics.fill(cx - 3, cy - 2, cx + 4, cy - 1, color);
            guiGraphics.fill(cx - 2, cy - 1, cx + 3, cy, color);
            guiGraphics.fill(cx - 1, cy, cx + 2, cy + 1, color);
            guiGraphics.fill(cx, cy + 1, cx + 1, cy + 2, color);
        }
    }

    private int getFirstVisibleRowIndex() {
        return Math.max(0, (int) Math.floor((this.scrollOffset - 3.0) / ROW_HEIGHT) - 1);
    }

    private int getLastVisibleRowIndex() {
        return Math.min(this.rowWidgets.size() - 1, (int) Math.ceil((this.scrollOffset + this.viewportHeight) / ROW_HEIGHT) + 1);
    }

    private int getScrollbarLeft() {
        return this.viewportLeft + this.viewportWidth - SCROLLBAR_WIDTH - 2;
    }

    private int getScrollbarThumbHeight(int trackHeight) {
        int total = Math.max(1, this.getContentTotalHeight());
        int inner = Math.max(1, this.viewportHeight - 2);
        return Mth.clamp((int) Math.round((double) inner / total * trackHeight), 14, trackHeight);
    }

    private int getScrollbarThumbOffset(int trackHeight, int thumbHeight, int maxScroll) {
        if (maxScroll <= 0 || trackHeight <= thumbHeight) {
            return 0;
        }
        return (int) Math.round((this.scrollOffset / maxScroll) * (trackHeight - thumbHeight));
    }

    private boolean isInsideViewport(double x, double y) {
        return x >= this.viewportLeft + 1 && x < this.viewportLeft + this.viewportWidth - 1
                && y >= this.viewportTop + 1 && y < this.viewportTop + this.viewportHeight - 1;
    }

    private boolean isInsideViewportContent(double x, double y) {
        int right = this.viewportLeft + this.viewportWidth - 1 - (SCROLLBAR_WIDTH + 2);
        return x >= this.viewportLeft + 1 && x < right
                && y >= this.viewportTop + 1 && y < this.viewportTop + this.viewportHeight - 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.rawJsonMode) {
            double mx = event.x();
            double my = event.y();
            int maxScroll = this.getMaxScroll();
            boolean hasScrollbar = maxScroll > 0;

            if (hasScrollbar && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                int trackLeft = this.getScrollbarLeft();
                int trackTop = this.viewportTop + 2;
                int trackHeight = Math.max(12, this.viewportHeight - 4);
                if (mx >= trackLeft - 1 && mx <= trackLeft + SCROLLBAR_WIDTH + 1
                        && my >= trackTop && my < trackTop + trackHeight) {
                    int thumbHeight = this.getScrollbarThumbHeight(trackHeight);
                    int thumbTop = trackTop + this.getScrollbarThumbOffset(trackHeight, thumbHeight, maxScroll);
                    this.draggingScrollbar = true;
                    if (my >= thumbTop && my < thumbTop + thumbHeight) {
                        this.scrollbarDragGrabY = my - thumbTop;
                    } else {
                        this.scrollbarDragGrabY = thumbHeight / 2.0;
                        this.updateScrollFromTrackMouse(my, trackTop, trackHeight, thumbHeight, maxScroll);
                    }
                    return true;
                }
            }

            if (this.isInsideViewportContent(mx, my)) {
                int vpBottom = this.viewportTop + this.viewportHeight;
                int firstVisible = this.getFirstVisibleRowIndex();
                int lastVisible = this.getLastVisibleRowIndex();
                for (int i = firstVisible; i <= lastVisible; i++) {
                    RowWidgets rw = this.rowWidgets.get(i);
                    int widgetTop = rw.editBox.getY();
                    if (widgetTop + ROW_WIDGET_HEIGHT < this.viewportTop || widgetTop > vpBottom) {
                        continue;
                    }
                    if (rw.editBox.mouseClicked(event, doubleClick)) {
                        this.setFocusedWidget(rw.editBox);
                        this.draggingRowWidget = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
                        return true;
                    }
                    if (rw.upButton.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                    if (rw.downButton.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                    if (rw.removeButton.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                }
                this.setFocusedWidget(null);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.rawJsonMode) {
            if (this.draggingScrollbar) {
                int maxScroll = this.getMaxScroll();
                int trackTop = this.viewportTop + 2;
                int trackHeight = Math.max(12, this.viewportHeight - 4);
                int thumbHeight = this.getScrollbarThumbHeight(trackHeight);
                this.updateScrollFromTrackMouse(event.y(), trackTop, trackHeight, thumbHeight, maxScroll);
                return true;
            }
            if (this.draggingRowWidget && this.getFocusedWidget() != null) {
                this.getFocusedWidget().mouseDragged(event, dragX, dragY);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = this.draggingScrollbar || this.draggingRowWidget;
        this.draggingScrollbar = false;
        this.draggingRowWidget = false;
        if (handled) {
            if (this.getFocusedWidget() != null) {
                this.getFocusedWidget().mouseReleased(event);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.rawJsonMode && this.isInsideViewport(mouseX, mouseY)) {
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 16.0, 0.0, maxScroll);
                this.repositionRowWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.rawJsonMode && event.key() == InputConstants.KEY_TAB) {
            boolean reverse = event.hasShiftDown();
            this.cycleStructuredFocus(reverse);
            return true;
        }
        return super.keyPressed(event);
    }

    private void cycleStructuredFocus(boolean reverse) {
        List<AbstractWidget> order = new ArrayList<>();
        if (this.addButton != null && this.addButton.visible && this.addButton.active) {
            order.add(this.addButton);
        }
        if (this.modeToggleButton != null && this.modeToggleButton.visible && this.modeToggleButton.active) {
            order.add(this.modeToggleButton);
        }
        for (RowWidgets rw : this.rowWidgets) {
            if (rw.editBox.visible && rw.editBox.active) {
                order.add(rw.editBox);
            }
        }
        if (this.resetButton != null && this.resetButton.visible && this.resetButton.active) {
            order.add(this.resetButton);
        }
        if (this.cancelButton != null && this.cancelButton.visible && this.cancelButton.active) {
            order.add(this.cancelButton);
        }
        if (this.applyButton != null && this.applyButton.visible && this.applyButton.active) {
            order.add(this.applyButton);
        }
        if (order.isEmpty()) {
            return;
        }

        int currentIdx = order.indexOf(this.getFocusedWidget());
        int nextIdx = currentIdx < 0
                ? (reverse ? order.size() - 1 : 0)
                : (currentIdx + (reverse ? -1 : 1) + order.size()) % order.size();
        AbstractWidget next = order.get(nextIdx);

        for (int i = 0; i < this.rowWidgets.size(); i++) {
            if (this.rowWidgets.get(i).editBox == next) {
                this.ensureRowVisible(i);
                break;
            }
        }
        this.setFocusedWidget(next);
    }

    private void updateScrollFromTrackMouse(double mouseY, int trackTop, int trackHeight, int thumbHeight, int maxScroll) {
        int movable = Math.max(1, trackHeight - thumbHeight);
        double thumbOffset = Mth.clamp(mouseY - trackTop - this.scrollbarDragGrabY, 0.0, movable);
        this.scrollOffset = Mth.clamp((thumbOffset / movable) * maxScroll, 0.0, maxScroll);
        this.repositionRowWidgets();
    }

    private static final class RowState<E> {
        private String text;
        private @Nullable E parsedValue;
        private boolean valid;
        private @Nullable String errorMessage;

        private RowState(String text) {
            this.text = text;
        }
    }

    private record RowWidgets(EditBox editBox, Button upButton, Button downButton, Button removeButton) {
    }
}
