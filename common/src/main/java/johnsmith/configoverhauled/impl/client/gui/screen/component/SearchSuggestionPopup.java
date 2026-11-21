package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Guided search autocomplete popup extending upward from the config search {@link EditBox}.
 * <p>
 * Supports scrolling via Up/Down arrows or Numpad 8/2 (NumLock navigation) and mouse wheel,
 * and autocompletion via Tab, Right Arrow (at token end), Numpad 6 (NumLock navigation), or click.
 */
public class SearchSuggestionPopup {
    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int ROW_HEIGHT = 14;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 2;
    private static final int SCROLLBAR_WIDTH = 3;

    private static final int BG_COLOR = 0xF2101010;
    private static final int BORDER_COLOR = 0xFFA0A0A0;
    private static final int SEPARATOR_COLOR = 0x55A0A0A0;
    private static final int SELECTED_BG_COLOR = 0x44FFFFFF;
    private static final int HOVER_BG_COLOR = 0x22FFFFFF;
    private static final int SELECTED_TEXT_COLOR = 0xFFFFFF55;
    private static final int NORMAL_TEXT_COLOR = 0xFFE0E0E0;
    private static final int HINT_TEXT_COLOR = 0xFF888888;
    private static final int SCROLLBAR_TRACK_COLOR = 0x44FFFFFF;
    private static final int SCROLLBAR_THUMB_COLOR = 0xCCFFFFFF;

    private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final Minecraft minecraft;
    private final Font font;
    private final EditBox searchBox;
    private final Supplier<ConfigList> activeListSupplier;

    private final List<Suggestion> suggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int activeTokenStart = 0;
    private int activeTokenEnd = 0;

    private boolean dismissed = false;
    private boolean wasFocused = false;
    private String lastText = "";
    private int lastCursor = -1;

    private int popupLeft;
    private int popupTop;
    private int popupWidth;
    private int popupHeight;

    private int suppressedChar = -1;
    private long suppressedCharUntilMs = 0L;

    public SearchSuggestionPopup(Minecraft minecraft, Font font, EditBox searchBox, Supplier<ConfigList> activeListSupplier) {
        this.minecraft = minecraft;
        this.font = font;
        this.searchBox = searchBox;
        this.activeListSupplier = activeListSupplier;
    }

    public void onSearchTextChanged() {
        this.dismissed = false;
        this.refreshSuggestions();
    }

    public void onTabChanged() {
        this.refreshSuggestions();
    }

    public boolean isVisible() {
        return this.searchBox.isFocused() && !this.dismissed && !this.suggestions.isEmpty();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isVisible()) {
            return false;
        }
        return mouseX >= this.popupLeft
                && mouseX < this.popupLeft + this.popupWidth
                && mouseY >= this.popupTop
                && mouseY < this.popupTop + this.popupHeight;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!this.searchBox.isFocused()) {
            return false;
        }

        this.syncIfNeeded();

        int key = event.key();
        int mods = event.modifiers();
        boolean numLockOff = (mods & GLFW.GLFW_MOD_NUM_LOCK) == 0;

        if (!this.isVisible()) {
            // Allow reopening dismissed suggestions with Up/Down or Tab while focused
            if (this.dismissed && !this.suggestions.isEmpty()
                    && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB
                    || (numLockOff && (key == GLFW.GLFW_KEY_KP_8 || key == GLFW.GLFW_KEY_KP_2 || key == GLFW.GLFW_KEY_KP_6)))) {
                this.dismissed = false;
                if (key == GLFW.GLFW_KEY_KP_8) this.suppressNextChar('8');
                else if (key == GLFW.GLFW_KEY_KP_2) this.suppressNextChar('2');
                else if (key == GLFW.GLFW_KEY_KP_6) this.suppressNextChar('6');
                return true;
            }
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.dismissed = true;
            return true;
        }

        if (key == GLFW.GLFW_KEY_UP || (key == GLFW.GLFW_KEY_KP_8 && numLockOff)) {
            if (key == GLFW.GLFW_KEY_KP_8) {
                this.suppressNextChar('8');
            }
            this.moveSelection(-1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_DOWN || (key == GLFW.GLFW_KEY_KP_2 && numLockOff)) {
            if (key == GLFW.GLFW_KEY_KP_2) {
                this.suppressNextChar('2');
            }
            this.moveSelection(1);
            return true;
        }

        boolean acceptKey = key == GLFW.GLFW_KEY_TAB
                || (key == GLFW.GLFW_KEY_KP_6 && numLockOff)
                || (key == GLFW.GLFW_KEY_RIGHT && this.searchBox.getCursorPosition() >= this.activeTokenEnd);

        if (acceptKey) {
            if (key == GLFW.GLFW_KEY_KP_6) {
                this.suppressNextChar('6');
            }
            this.applySelectedSuggestion();
            return true;
        }

        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (this.suppressedChar != -1) {
            long now = System.currentTimeMillis();
            int expected = this.suppressedChar;
            this.suppressedChar = -1;
            if (now <= this.suppressedCharUntilMs && event.codepoint() == expected) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!this.isVisible() || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        int rowIndex = this.getRowIndexAt(event.y());
        if (rowIndex >= 0 && rowIndex < this.suggestions.size()) {
            this.selectedIndex = rowIndex;
            this.applySelectedSuggestion();
            this.searchBox.setFocused(true);
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (this.suggestions.size() <= MAX_VISIBLE_ITEMS) {
            return true;
        }
        int delta = scrollY > 0 ? -1 : 1;
        int maxScroll = Math.max(0, this.suggestions.size() - MAX_VISIBLE_ITEMS);
        this.scrollOffset = Mth.clamp(this.scrollOffset + delta, 0, maxScroll);
        return true;
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        this.syncIfNeeded();
        if (!this.isVisible()) {
            return;
        }

        int visibleCount = Math.min(this.suggestions.size(), MAX_VISIBLE_ITEMS);
        int contentHeight = visibleCount * ROW_HEIGHT;

        this.popupLeft = this.searchBox.getX();
        this.popupWidth = this.searchBox.getWidth();
        this.popupHeight = contentHeight + PADDING_Y * 2;
        this.popupTop = Math.max(4, this.searchBox.getY() - this.popupHeight);

        int right = this.popupLeft + this.popupWidth;
        int bottom = this.popupTop + this.popupHeight;

        guiGraphics.nextStratum();

        // Upward extension panel connected to the top edge of the search EditBox
        guiGraphics.fill(this.popupLeft, this.popupTop, right, bottom, BG_COLOR);
        guiGraphics.fill(this.popupLeft, this.popupTop, right, this.popupTop + 1, BORDER_COLOR);
        guiGraphics.fill(this.popupLeft, this.popupTop, this.popupLeft + 1, bottom, BORDER_COLOR);
        guiGraphics.fill(right - 1, this.popupTop, right, bottom, BORDER_COLOR);
        guiGraphics.fill(this.popupLeft + 1, bottom - 1, right - 1, bottom, SEPARATOR_COLOR);

        boolean hasScrollbar = this.suggestions.size() > MAX_VISIBLE_ITEMS;
        int textRightLimit = right - PADDING_X - (hasScrollbar ? SCROLLBAR_WIDTH + 3 : 0);

        int hoveredIndex = this.isMouseOver(mouseX, mouseY) ? this.getRowIndexAt(mouseY) : -1;

        guiGraphics.enableScissor(this.popupLeft + 1, this.popupTop + 1, right - 1, bottom - 1);
        for (int i = 0; i < visibleCount; i++) {
            int suggestionIdx = this.scrollOffset + i;
            if (suggestionIdx >= this.suggestions.size()) {
                break;
            }

            Suggestion suggestion = this.suggestions.get(suggestionIdx);
            int rowTop = this.popupTop + PADDING_Y + i * ROW_HEIGHT;
            int rowBottom = rowTop + ROW_HEIGHT;

            if (suggestionIdx == this.selectedIndex) {
                guiGraphics.fill(this.popupLeft + 1, rowTop, right - 1, rowBottom, SELECTED_BG_COLOR);
            } else if (suggestionIdx == hoveredIndex) {
                guiGraphics.fill(this.popupLeft + 1, rowTop, right - 1, rowBottom, HOVER_BG_COLOR);
            }

            int textY = rowTop + (ROW_HEIGHT - this.font.lineHeight) / 2;
            int textColor = suggestionIdx == this.selectedIndex ? SELECTED_TEXT_COLOR : NORMAL_TEXT_COLOR;

            int primaryX = this.popupLeft + PADDING_X;
            int availableWidth = Math.max(0, textRightLimit - primaryX);
            String primaryText = this.trimToWidth(suggestion.display(), availableWidth);
            guiGraphics.text(this.font, primaryText, primaryX, textY, textColor, true);

            if (!suggestion.hint().isEmpty()) {
                int primaryWidth = this.font.width(primaryText);
                int remainingWidth = availableWidth - primaryWidth - 8;
                if (remainingWidth > 20) {
                    String trimmedHint = this.trimToWidth(suggestion.hint(), remainingWidth);
                    int hintWidth = this.font.width(trimmedHint);
                    int hintX = textRightLimit - hintWidth;
                    guiGraphics.text(this.font, trimmedHint, hintX, textY, HINT_TEXT_COLOR, false);
                }
            }
        }
        guiGraphics.disableScissor();

        if (hasScrollbar) {
            int trackX = right - SCROLLBAR_WIDTH - 2;
            int trackTop = this.popupTop + PADDING_Y;
            int trackHeight = contentHeight;
            guiGraphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackTop + trackHeight, SCROLLBAR_TRACK_COLOR);

            int maxScroll = this.suggestions.size() - MAX_VISIBLE_ITEMS;
            int thumbHeight = Math.max(12, (int) ((float) visibleCount / this.suggestions.size() * trackHeight));
            int thumbOffset = maxScroll > 0
                    ? (int) ((float) this.scrollOffset / maxScroll * (trackHeight - thumbHeight))
                    : 0;
            guiGraphics.fill(trackX, trackTop + thumbOffset, trackX + SCROLLBAR_WIDTH, trackTop + thumbOffset + thumbHeight, SCROLLBAR_THUMB_COLOR);
        }
    }

    private void syncIfNeeded() {
        boolean focused = this.searchBox.isFocused();
        String text = this.searchBox.getValue();
        int cursor = this.searchBox.getCursorPosition();

        if (focused && !this.wasFocused) {
            this.dismissed = false;
            this.refreshSuggestions();
        } else if (focused && (!text.equals(this.lastText) || cursor != this.lastCursor)) {
            this.refreshSuggestions();
        } else if (!focused && this.wasFocused) {
            this.dismissed = false;
        }

        this.wasFocused = focused;
        this.lastText = text;
        this.lastCursor = cursor;
    }

    private void moveSelection(int delta) {
        if (this.suggestions.isEmpty()) {
            return;
        }
        int size = this.suggestions.size();
        this.selectedIndex = (this.selectedIndex + delta + size) % size;
        this.ensureSelectionVisible();
    }

    private void ensureSelectionVisible() {
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + MAX_VISIBLE_ITEMS) {
            this.scrollOffset = this.selectedIndex - MAX_VISIBLE_ITEMS + 1;
        }
        int maxScroll = Math.max(0, this.suggestions.size() - MAX_VISIBLE_ITEMS);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);
    }

    private void applySelectedSuggestion() {
        if (this.suggestions.isEmpty()) {
            return;
        }
        int index = Mth.clamp(this.selectedIndex, 0, this.suggestions.size() - 1);
        Suggestion selected = this.suggestions.get(index);

        String text = this.searchBox.getValue();
        int start = Mth.clamp(this.activeTokenStart, 0, text.length());
        int end = Mth.clamp(this.activeTokenEnd, start, text.length());

        String replacement = selected.replacement();
        String after = text.substring(end);
        if (replacement.endsWith(" ") && after.startsWith(" ")) {
            replacement = replacement.substring(0, replacement.length() - 1);
        }

        String updated = text.substring(0, start) + replacement + after;
        int newCursor = start + replacement.length();

        this.searchBox.setValue(updated);
        this.searchBox.setCursorPosition(newCursor);
        this.searchBox.setHighlightPos(newCursor);
        this.dismissed = false;
        this.refreshSuggestions();
    }

    private int getRowIndexAt(double mouseY) {
        int relativeY = (int) mouseY - (this.popupTop + PADDING_Y);
        if (relativeY < 0) {
            return -1;
        }
        int row = relativeY / ROW_HEIGHT;
        int visibleCount = Math.min(this.suggestions.size(), MAX_VISIBLE_ITEMS);
        if (row < 0 || row >= visibleCount) {
            return -1;
        }
        return this.scrollOffset + row;
    }

    private void suppressNextChar(char ch) {
        this.suppressedChar = ch;
        this.suppressedCharUntilMs = System.currentTimeMillis() + 50L;
    }

    private void refreshSuggestions() {
        String text = this.searchBox.getValue();
        int cursor = Mth.clamp(this.searchBox.getCursorPosition(), 0, text.length());

        TokenSpan span = findTokenAtCursor(text, cursor);
        this.activeTokenStart = span.start;
        this.activeTokenEnd = span.end;

        String activeToken = text.substring(span.start, span.end);
        String otherQueryText = (text.substring(0, span.start) + " " + text.substring(span.end)).trim();
        ConfigSearchQuery otherQuery = ConfigSearchQuery.parse(otherQueryText);

        this.suggestions.clear();
        this.populateSuggestions(activeToken, otherQuery);

        this.selectedIndex = 0;
        this.scrollOffset = 0;
    }

    private void populateSuggestions(String activeToken, ConfigSearchQuery otherQuery) {
        List<TagSpec> tagSpecs = List.of(
                new TagSpec("#group:", "#group:", Component.translatable("config_overhauled.config.search.tag.group").getString()),
                new TagSpec("#id:", "#id:", Component.translatable("config_overhauled.config.search.tag.id").getString()),
                new TagSpec("#key:", "#key:", Component.translatable("config_overhauled.config.search.tag.key").getString()),
                new TagSpec("#word:", "#word:", Component.translatable("config_overhauled.config.search.tag.word").getString()),
                new TagSpec("#scope:", "#scope:", Component.translatable("config_overhauled.config.search.tag.scope").getString()),
                new TagSpec("#modified", "#modified ", Component.translatable("config_overhauled.config.search.tag.modified").getString()),
                new TagSpec("#locked", "#locked ", Component.translatable("config_overhauled.config.search.tag.locked").getString())
        );

        if (activeToken.isEmpty()) {
            for (TagSpec tag : tagSpecs) {
                this.suggestions.add(new Suggestion(tag.display, tag.replacement, tag.hint));
            }
            return;
        }

        if (activeToken.startsWith("#")) {
            int colonIdx = activeToken.indexOf(':');
            if (colonIdx < 0) {
                String lowerToken = activeToken.toLowerCase(Locale.ROOT);
                for (TagSpec tag : tagSpecs) {
                    if (tag.display.startsWith(lowerToken)) {
                        this.suggestions.add(new Suggestion(tag.display, tag.replacement, tag.hint));
                    }
                }
                if (lowerToken.equals("#modified")) {
                    this.populateBooleanTagSuggestions("modified", "");
                } else if (lowerToken.equals("#locked")) {
                    this.populateBooleanTagSuggestions("locked", "");
                }
                return;
            }

            String tagName = activeToken.substring(1, colonIdx).toLowerCase(Locale.ROOT);
            String typedValue = stripQuotes(activeToken.substring(colonIdx + 1)).toLowerCase(Locale.ROOT);

            switch (tagName) {
                case "group" -> this.populateGroupSuggestions(typedValue, otherQuery);
                case "id" -> this.populateIdSuggestions(typedValue, otherQuery);
                case "key" -> this.populateKeySuggestions(typedValue, otherQuery);
                case "word" -> this.populateWordSuggestions(typedValue, otherQuery);
                case "scope" -> this.populateScopeSuggestions(typedValue, otherQuery);
                case "modified" -> this.populateBooleanTagSuggestions("modified", typedValue);
                case "locked" -> this.populateBooleanTagSuggestions("locked", typedValue);
                default -> {}
            }
            return;
        }

        String lowerPlain = stripQuotes(activeToken).toLowerCase(Locale.ROOT);
        this.populatePlainSuggestions(lowerPlain, otherQuery);
    }

    private void populateScopeSuggestions(String typedValue, ConfigSearchQuery otherQuery) {
        List<Property<?>> properties = this.collectVisibleMatchingProperties(otherQuery);
        Set<ConfigScope> presentScopes = new LinkedHashSet<>();
        for (Property<?> property : properties) {
            presentScopes.add(property.scope());
        }

        for (ConfigScope scope : ConfigScope.values()) {
            if (!presentScopes.contains(scope)) {
                continue;
            }
            String lowerName = scope.name().toLowerCase(Locale.ROOT);
            if (typedValue.isEmpty() || lowerName.startsWith(typedValue) || lowerName.contains(typedValue)) {
                String token = "#scope:" + lowerName;
                this.suggestions.add(new Suggestion(token, token + " ", scope.name()));
            }
        }
    }

    private void populateBooleanTagSuggestions(String tagName, String typedValue) {
        for (String val : List.of("true", "false")) {
            if (typedValue.isEmpty() || val.startsWith(typedValue)) {
                String token = "#" + tagName + ":" + val;
                this.suggestions.add(new Suggestion(token, token + " ", ""));
            }
        }
    }

    private void populateGroupSuggestions(String typedValue, ConfigSearchQuery otherQuery) {
        ConfigList list = this.activeListSupplier.get();
        if (list == null) return;

        Category category = list.getCategory();
        ConfigManager manager = list.getManager();
        boolean isIngame = this.minecraft.player != null;

        List<Group> groups = manager.getGroupsIn(category);
        if (groups == null) return;

        List<Suggestion> startsWithMatches = new ArrayList<>();
        List<Suggestion> containsMatches = new ArrayList<>();

        for (Group group : groups) {
            List<Property<?>> properties = manager.getPropertiesIn(group);
            if (properties == null) continue;

            boolean hasVisibleMatch = false;
            for (Property<?> property : properties) {
                if (property.scope() == ConfigScope.LEVEL && !isIngame) continue;
                if (otherQuery.matches(group, property)) {
                    hasVisibleMatch = true;
                    break;
                }
            }
            if (!hasVisibleMatch) continue;

            String groupId = group.id();
            String translated = Component.translatable(group.translationKey()).getString();
            String lowerId = groupId.toLowerCase(Locale.ROOT);
            String lowerTranslated = translated.toLowerCase(Locale.ROOT);

            if (typedValue.isEmpty() || lowerId.startsWith(typedValue) || lowerTranslated.startsWith(typedValue)) {
                String token = "#group:" + groupId;
                startsWithMatches.add(new Suggestion(token, token + " ", translated));
            } else if (lowerId.contains(typedValue) || lowerTranslated.contains(typedValue)) {
                String token = "#group:" + groupId;
                containsMatches.add(new Suggestion(token, token + " ", translated));
            }
        }

        this.suggestions.addAll(startsWithMatches);
        this.suggestions.addAll(containsMatches);
    }

    private void populateIdSuggestions(String typedValue, ConfigSearchQuery otherQuery) {
        List<Property<?>> properties = this.collectVisibleMatchingProperties(otherQuery);
        List<Suggestion> startsWithMatches = new ArrayList<>();
        List<Suggestion> containsMatches = new ArrayList<>();

        for (Property<?> property : properties) {
            String resourceName = property.resourceName();
            String lowerName = resourceName.toLowerCase(Locale.ROOT);
            String lowerUnique = property.getUniqueId().toLowerCase(Locale.ROOT);
            String translated = Component.translatable(property.translationKey()).getString();

            if (typedValue.isEmpty() || lowerName.startsWith(typedValue)) {
                String token = "#id:" + resourceName;
                startsWithMatches.add(new Suggestion(token, token + " ", translated));
            } else if (lowerName.contains(typedValue) || lowerUnique.contains(typedValue)) {
                String token = "#id:" + resourceName;
                containsMatches.add(new Suggestion(token, token + " ", translated));
            }
        }

        this.suggestions.addAll(startsWithMatches);
        this.suggestions.addAll(containsMatches);
    }

    private void populateKeySuggestions(String typedValue, ConfigSearchQuery otherQuery) {
        List<Property<?>> properties = this.collectVisibleMatchingProperties(otherQuery);
        List<Suggestion> startsWithMatches = new ArrayList<>();
        List<Suggestion> containsMatches = new ArrayList<>();

        for (Property<?> property : properties) {
            String translationKey = property.translationKey();
            String lowerKey = translationKey.toLowerCase(Locale.ROOT);
            String lowerResource = property.resourceName().toLowerCase(Locale.ROOT);
            String translated = Component.translatable(translationKey).getString();

            if (typedValue.isEmpty() || lowerKey.startsWith(typedValue) || lowerResource.startsWith(typedValue)) {
                String token = "#key:" + translationKey;
                startsWithMatches.add(new Suggestion(token, token + " ", translated));
            } else if (lowerKey.contains(typedValue) || lowerResource.contains(typedValue)) {
                String token = "#key:" + translationKey;
                containsMatches.add(new Suggestion(token, token + " ", translated));
            }
        }

        this.suggestions.addAll(startsWithMatches);
        this.suggestions.addAll(containsMatches);
    }

    private void populateWordSuggestions(String typedValue, ConfigSearchQuery otherQuery) {
        List<Property<?>> properties = this.collectVisibleMatchingProperties(otherQuery);
        Set<String> uniqueWords = new LinkedHashSet<>();
        Language language = Language.getInstance();

        for (Property<?> property : properties) {
            collectWords(Component.translatable(property.translationKey()).getString(), uniqueWords);
            String descKey = property.descriptionTranslationKey();
            if (language.has(descKey)) {
                collectWords(Component.translatable(descKey).getString(), uniqueWords);
            }
        }

        List<String> startsWithMatches = new ArrayList<>();
        List<String> containsMatches = new ArrayList<>();

        for (String word : uniqueWords) {
            if (typedValue.isEmpty() || word.startsWith(typedValue)) {
                startsWithMatches.add(word);
            } else if (word.contains(typedValue)) {
                containsMatches.add(word);
            }
        }

        startsWithMatches.sort(Comparator.naturalOrder());
        containsMatches.sort(Comparator.naturalOrder());

        for (String word : startsWithMatches) {
            String token = "#word:" + word;
            this.suggestions.add(new Suggestion(token, token + " ", ""));
        }
        for (String word : containsMatches) {
            String token = "#word:" + word;
            this.suggestions.add(new Suggestion(token, token + " ", ""));
        }
    }

    private void populatePlainSuggestions(String lowerPlain, ConfigSearchQuery otherQuery) {
        List<Property<?>> properties = this.collectVisibleMatchingProperties(otherQuery);
        List<Suggestion> startsWithMatches = new ArrayList<>();
        List<Suggestion> containsMatches = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Property<?> property : properties) {
            String translated = Component.translatable(property.translationKey()).getString();
            String lowerTranslated = translated.toLowerCase(Locale.ROOT);
            String lowerResource = property.resourceName().toLowerCase(Locale.ROOT);

            if (!seen.add(lowerTranslated)) {
                continue;
            }

            String replacement = translated.indexOf(' ') >= 0
                    ? "\"" + translated + "\" "
                    : translated + " ";
            String groupHint = Component.translatable(property.parentGroup().translationKey()).getString();

            if (lowerTranslated.startsWith(lowerPlain) || lowerResource.startsWith(lowerPlain)) {
                startsWithMatches.add(new Suggestion(translated, replacement, groupHint));
            } else if (lowerTranslated.contains(lowerPlain) || lowerResource.contains(lowerPlain)) {
                containsMatches.add(new Suggestion(translated, replacement, groupHint));
            }
        }

        this.suggestions.addAll(startsWithMatches);
        this.suggestions.addAll(containsMatches);
    }

    private List<Property<?>> collectVisibleMatchingProperties(ConfigSearchQuery otherQuery) {
        List<Property<?>> result = new ArrayList<>();
        ConfigList list = this.activeListSupplier.get();
        if (list == null) return result;

        Category category = list.getCategory();
        ConfigManager manager = list.getManager();
        boolean isIngame = this.minecraft.player != null;

        List<Group> groups = manager.getGroupsIn(category);
        if (groups == null) return result;

        for (Group group : groups) {
            List<Property<?>> properties = manager.getPropertiesIn(group);
            if (properties == null) continue;
            for (Property<?> property : properties) {
                if (property.scope() == ConfigScope.LEVEL && !isIngame) continue;
                if (otherQuery.matches(group, property)) {
                    result.add(property);
                }
            }
        }
        return result;
    }

    private static void collectWords(String text, Set<String> out) {
        for (String raw : WORD_SPLIT_PATTERN.split(text.toLowerCase(Locale.ROOT))) {
            if (raw.length() >= 2) {
                out.add(raw);
            }
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = this.font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return "...";
        }
        return this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    private static TokenSpan findTokenAtCursor(String text, int cursor) {
        boolean inQuotes = false;
        int tokenStart = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (tokenStart < 0) {
                    tokenStart = i;
                }
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (tokenStart >= 0) {
                    if (cursor >= tokenStart && cursor <= i) {
                        return new TokenSpan(tokenStart, i);
                    }
                    tokenStart = -1;
                }
            } else {
                if (tokenStart < 0) {
                    tokenStart = i;
                }
            }
        }

        if (tokenStart >= 0 && cursor >= tokenStart && cursor <= text.length()) {
            return new TokenSpan(tokenStart, text.length());
        }
        return new TokenSpan(cursor, cursor);
    }

    private static String stripQuotes(String value) {
        if (value.isEmpty()) return value;
        String result = value;
        if (result.startsWith("\"")) result = result.substring(1);
        if (result.endsWith("\"")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private record TokenSpan(int start, int end) {}
    private record TagSpec(String display, String replacement, String hint) {}
    private record Suggestion(String display, String replacement, String hint) {}
}
