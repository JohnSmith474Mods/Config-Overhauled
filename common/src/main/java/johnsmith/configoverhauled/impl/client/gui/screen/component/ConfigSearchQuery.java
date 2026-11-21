package johnsmith.configoverhauled.impl.client.gui.screen.component;

import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.entry.OptionEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parses and evaluates tagged search queries for the configuration UI.
 * <p>
 * Supported tags:
 * <ul>
 *     <li>{@code #group:<term>} — matches the group's translated name or {@link Group#id()}</li>
 *     <li>{@code #id:<term>} — matches the property's internal id ({@link Property#resourceName()} or {@link Property#getUniqueId()})</li>
 *     <li>{@code #key:<term>} — matches the property's translation key ({@link Property#translationKey()}) or {@link Property#resourceName()}</li>
 *     <li>{@code #word:<term>} — matches words in the property's translated name and description/tooltip</li>
 *     <li>{@code #scope:<term>} — matches the property's {@link Property#scope()} ({@code client}, {@code global}, {@code level})</li>
 *     <li>{@code #modified} / {@code #modified:<true|false>} — matches properties differing from their default value</li>
 *     <li>{@code #locked} / {@code #locked:<true|false>} — matches properties locked by the remote server</li>
 * </ul>
 * Untyped terms match against the property's translated name, translation key, or resource name.
 * Multiple tags and untyped terms may be combined in a single query (logical AND).
 */
public final class ConfigSearchQuery {
    public static final ConfigSearchQuery EMPTY = new ConfigSearchQuery(Collections.emptyList());

    private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final List<String> KNOWN_TAG_PREFIXES = List.of(
            "#group:", "#id:", "#key:", "#word:", "#scope:", "#modified:", "#locked:"
    );

    private final List<SearchTerm> terms;

    private ConfigSearchQuery(List<SearchTerm> terms) {
        this.terms = terms;
    }

    public static ConfigSearchQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return EMPTY;
        }

        List<String> tokens = tokenize(rawQuery.trim());
        if (tokens.isEmpty()) {
            return EMPTY;
        }

        List<SearchTerm> parsed = new ArrayList<>();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            if (token.startsWith("#")) {
                int colonIndex = token.indexOf(':');
                if (colonIndex > 0) {
                    String tagName = token.substring(1, colonIndex).toLowerCase(Locale.ROOT);
                    String value = stripQuotes(token.substring(colonIndex + 1)).trim().toLowerCase(Locale.ROOT);

                    TagType tagType = TagType.fromName(tagName);
                    if (tagType != null) {
                        // Gracefully ignore incomplete/empty tag values such as "#group:" while typing.
                        if (!value.isEmpty()) {
                            parsed.add(new SearchTerm(tagType, value));
                        }
                        continue;
                    }
                } else {
                    String lowerToken = token.toLowerCase(Locale.ROOT);
                    if (lowerToken.equals("#modified")) {
                        parsed.add(new SearchTerm(TagType.MODIFIED, "true"));
                        continue;
                    }
                    if (lowerToken.equals("#locked")) {
                        parsed.add(new SearchTerm(TagType.LOCKED, "true"));
                        continue;
                    }
                    if (isIncompleteTagPrefix(lowerToken)) {
                        // Gracefully ignore incomplete tag prefixes (e.g. "#", "#gr") while typing.
                        continue;
                    }
                }
            }

            String plainValue = stripQuotes(token).trim().toLowerCase(Locale.ROOT);
            if (!plainValue.isEmpty()) {
                parsed.add(new SearchTerm(TagType.PLAIN, plainValue));
            }
        }

        return parsed.isEmpty() ? EMPTY : new ConfigSearchQuery(Collections.unmodifiableList(parsed));
    }

    public boolean isEmpty() {
        return this.terms.isEmpty();
    }

    public boolean matches(Group group, Property<?> property) {
        if (this.terms.isEmpty()) {
            return true;
        }
        return this.matches(group, property, OptionEntry.determineLockState(Minecraft.getInstance(), property));
    }

    public boolean matches(Group group, Property<?> property, boolean isLocked) {
        if (this.terms.isEmpty()) {
            return true;
        }

        String translatedName = null;
        String translatedGroup = null;
        String translatedDescription = null;
        String[] nameAndDescWords = null;

        for (SearchTerm term : this.terms) {
            switch (term.type) {
                case GROUP -> {
                    if (group == null) {
                        return false;
                    }
                    if (translatedGroup == null) {
                        translatedGroup = Component.translatable(group.translationKey()).getString().toLowerCase(Locale.ROOT);
                    }
                    String groupId = group.id().toLowerCase(Locale.ROOT);
                    if (!translatedGroup.contains(term.value) && !groupId.contains(term.value)) {
                        return false;
                    }
                }
                case ID -> {
                    String resourceName = property.resourceName().toLowerCase(Locale.ROOT);
                    String uniqueId = property.getUniqueId().toLowerCase(Locale.ROOT);
                    if (!resourceName.contains(term.value) && !uniqueId.contains(term.value)) {
                        return false;
                    }
                }
                case KEY -> {
                    String translationKey = property.translationKey().toLowerCase(Locale.ROOT);
                    String resourceName = property.resourceName().toLowerCase(Locale.ROOT);
                    if (!translationKey.contains(term.value) && !resourceName.contains(term.value)) {
                        return false;
                    }
                }
                case WORD -> {
                    if (translatedName == null) {
                        translatedName = Component.translatable(property.translationKey()).getString().toLowerCase(Locale.ROOT);
                    }
                    if (translatedDescription == null) {
                        String descKey = property.descriptionTranslationKey();
                        if (Language.getInstance().has(descKey)) {
                            translatedDescription = Component.translatable(descKey).getString().toLowerCase(Locale.ROOT);
                        } else {
                            translatedDescription = "";
                        }
                    }
                    if (nameAndDescWords == null) {
                        String combined = translatedDescription.isEmpty()
                                ? translatedName
                                : translatedName + " " + translatedDescription;
                        nameAndDescWords = WORD_SPLIT_PATTERN.split(combined);
                    }
                    if (!matchesWordTerm(nameAndDescWords, term.value)) {
                        return false;
                    }
                }
                case SCOPE -> {
                    String scopeName = property.scope().name().toLowerCase(Locale.ROOT);
                    if (!scopeName.contains(term.value)) {
                        return false;
                    }
                }
                case MODIFIED -> {
                    boolean isModified = !Objects.equals(property.get(), property.defaultValue());
                    if (!matchesBooleanTerm(isModified, term.value)) {
                        return false;
                    }
                }
                case LOCKED -> {
                    if (!matchesBooleanTerm(isLocked, term.value)) {
                        return false;
                    }
                }
                case PLAIN -> {
                    if (translatedName == null) {
                        translatedName = Component.translatable(property.translationKey()).getString().toLowerCase(Locale.ROOT);
                    }
                    String translationKey = property.translationKey().toLowerCase(Locale.ROOT);
                    String resourceName = property.resourceName().toLowerCase(Locale.ROOT);
                    if (!translatedName.contains(term.value)
                            && !translationKey.contains(term.value)
                            && !resourceName.contains(term.value)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean matchesBooleanTerm(boolean actual, String queryValue) {
        if ("true".startsWith(queryValue)) {
            return actual;
        }
        if ("false".startsWith(queryValue)) {
            return !actual;
        }
        return false;
    }

    private static boolean matchesWordTerm(String[] words, String value) {
        String[] queryWords = WORD_SPLIT_PATTERN.split(value);
        List<String> nonEmptyQueryWords = new ArrayList<>();
        for (String qw : queryWords) {
            if (!qw.isEmpty()) {
                nonEmptyQueryWords.add(qw);
            }
        }
        if (nonEmptyQueryWords.isEmpty()) {
            return true;
        }

        for (String qw : nonEmptyQueryWords) {
            boolean matched = false;
            for (String word : words) {
                if (!word.isEmpty() && word.startsWith(qw)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIncompleteTagPrefix(String lowerToken) {
        for (String prefix : KNOWN_TAG_PREFIXES) {
            if (prefix.startsWith(lowerToken)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static String stripQuotes(String value) {
        if (value.isEmpty()) {
            return value;
        }
        String result = value;
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private enum TagType {
        GROUP,
        ID,
        KEY,
        WORD,
        SCOPE,
        MODIFIED,
        LOCKED,
        PLAIN;

        private static TagType fromName(String name) {
            return switch (name) {
                case "group" -> GROUP;
                case "id" -> ID;
                case "key" -> KEY;
                case "word" -> WORD;
                case "scope" -> SCOPE;
                case "modified" -> MODIFIED;
                case "locked" -> LOCKED;
                default -> null;
            };
        }
    }

    private record SearchTerm(TagType type, String value) {}
}
