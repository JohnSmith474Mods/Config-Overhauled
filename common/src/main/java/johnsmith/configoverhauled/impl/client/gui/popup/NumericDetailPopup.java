package johnsmith.configoverhauled.impl.client.gui.popup;

import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Detailed modal editor for bounded numeric properties ({@code Integer}, {@code Long}, {@code Float}, {@code Double}).
 * <p>
 * Provides live numeric and range validation, explicit out-of-bounds error messages, and an optional
 * bidirectionally synchronized slider whenever the property defines a finite, non-sentinel numeric range.
 *
 * @param <T> The numeric value type.
 */
public class NumericDetailPopup<T extends Number & Comparable<T>> extends TextDetailPopup<T> {
    private static final double MAX_SLIDER_SPAN = 1_000_000_000_000.0;

    private final DefaultProperty.Bounded<T> boundedProperty;
    private final Function<String, T> parser;
    private final Predicate<String> partialPredicate;
    private final String regexFilter;
    private final Function<T, String> formatter;
    private final boolean hasSlider;

    private @Nullable NumericSlider slider;
    private boolean syncingSlider = false;

    public NumericDetailPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            DefaultProperty.Bounded<T> property,
            @Nullable String initialText,
            Function<String, T> parser,
            Predicate<String> partialPredicate,
            String regexFilter,
            Function<T, String> formatter,
            boolean isLocked,
            Consumer<T> onApply
    ) {
        super(parentScreen, minecraft, property, initialText, isLocked, onApply);
        this.boundedProperty = property;
        this.parser = parser;
        this.partialPredicate = partialPredicate;
        this.regexFilter = regexFilter;
        this.formatter = formatter;
        this.hasSlider = hasUsableSliderRange(property.lowerBound, property.upperBound);
    }

    @Override
    protected int getPreferredHeight() {
        return this.hasSlider ? 194 : 164;
    }

    @Override
    protected String getRegexFilter() {
        return this.regexFilter;
    }

    @Override
    protected String formatValue(T value) {
        return this.formatter.apply(value);
    }

    @Override
    protected ValidationResult<T> validateInput(String text) {
        if (this.partialPredicate.test(text)) {
            return ValidationResult.error(Component.translatable("config_overhauled.popup.numeric.incomplete"));
        }
        try {
            T parsed = this.parser.apply(text);
            if (parsed instanceof Float f && !Float.isFinite(f)) {
                return ValidationResult.error(Component.translatable("config_overhauled.popup.numeric.invalid"));
            }
            if (parsed instanceof Double d && !Double.isFinite(d)) {
                return ValidationResult.error(Component.translatable("config_overhauled.popup.numeric.invalid"));
            }
            if (parsed.compareTo(this.boundedProperty.lowerBound) < 0
                    || parsed.compareTo(this.boundedProperty.upperBound) > 0) {
                return ValidationResult.error(Component.translatable(
                        "config_overhauled.popup.numeric.out_of_range",
                        this.formatValue(this.boundedProperty.lowerBound),
                        this.formatValue(this.boundedProperty.upperBound)
                ));
            }
            return ValidationResult.ok(parsed);
        } catch (NumberFormatException e) {
            return ValidationResult.error(Component.translatable("config_overhauled.popup.numeric.invalid"));
        }
    }

    @Override
    protected @Nullable Component getRightInfoText() {
        if (!hasExplicitFiniteBounds(this.boundedProperty.lowerBound, this.boundedProperty.upperBound)) {
            return Component.translatable("config_overhauled.popup.numeric.unbounded");
        }
        return Component.translatable("config_overhauled.word.range")
                .append(Component.literal(": [ "
                        + this.formatValue(this.boundedProperty.lowerBound)
                        + " - "
                        + this.formatValue(this.boundedProperty.upperBound)
                        + " ]"));
    }

    @Override
    protected void initExtraContent(int left, int top, int width, int height) {
        if (!this.hasSlider) {
            this.slider = null;
            return;
        }

        double initialRatio = this.computeNormalizedRatio(this.resolveCurrentNumericForSlider());
        this.slider = new NumericSlider(left, top, width, 20, initialRatio);
        this.slider.active = !this.isLocked;
        this.addRenderableWidget(this.slider);
    }

    @Override
    protected void onWorkingValueChanged(@Nullable T newValue, boolean valid) {
        if (this.slider == null || this.syncingSlider || !valid || newValue == null) {
            return;
        }
        this.slider.syncFromValue(newValue);
    }

    private T resolveCurrentNumericForSlider() {
        if (this.parsedValue != null) {
            return this.parsedValue;
        }
        try {
            if (!this.partialPredicate.test(this.workingText)) {
                T candidate = this.parser.apply(this.workingText);
                if (candidate.compareTo(this.boundedProperty.lowerBound) >= 0
                        && candidate.compareTo(this.boundedProperty.upperBound) <= 0) {
                    return candidate;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return this.boundedProperty.get();
    }

    private double computeNormalizedRatio(T value) {
        double min = this.boundedProperty.lowerBound.doubleValue();
        double max = this.boundedProperty.upperBound.doubleValue();
        if (max <= min) {
            return 0.0;
        }
        return Mth.clamp((value.doubleValue() - min) / (max - min), 0.0, 1.0);
    }

    @SuppressWarnings("unchecked")
    private T mapRatioToValue(double ratio) {
        double clampedRatio = Mth.clamp(ratio, 0.0, 1.0);
        double min = this.boundedProperty.lowerBound.doubleValue();
        double max = this.boundedProperty.upperBound.doubleValue();
        double raw = min + clampedRatio * (max - min);

        T lower = this.boundedProperty.lowerBound;
        T upper = this.boundedProperty.upperBound;

        if (lower instanceof Integer) {
            int val = (int) Math.round(raw);
            val = Math.max((Integer) lower, Math.min((Integer) upper, val));
            return (T) Integer.valueOf(val);
        }
        if (lower instanceof Long) {
            long val = Math.round(raw);
            val = Math.max((Long) lower, Math.min((Long) upper, val));
            return (T) Long.valueOf(val);
        }
        if (lower instanceof Float) {
            float val = (float) roundFloating(raw, max - min);
            val = Math.max((Float) lower, Math.min((Float) upper, val));
            return (T) Float.valueOf(val);
        }
        if (lower instanceof Double) {
            double val = roundFloating(raw, max - min);
            val = Math.max((Double) lower, Math.min((Double) upper, val));
            return (T) Double.valueOf(val);
        }

        return this.boundedProperty.get();
    }

    private static double roundFloating(double raw, double span) {
        int scale;
        if (span <= 1.0) {
            scale = 4;
        } else if (span <= 10.0) {
            scale = 3;
        } else if (span <= 1000.0) {
            scale = 2;
        } else {
            scale = 1;
        }
        return BigDecimal.valueOf(raw).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static <T extends Number & Comparable<T>> boolean hasExplicitFiniteBounds(T lower, T upper) {
        if (lower instanceof Integer min && upper instanceof Integer max) {
            return !(min == Integer.MIN_VALUE && max == Integer.MAX_VALUE);
        }
        if (lower instanceof Long min && upper instanceof Long max) {
            return !(min == Long.MIN_VALUE && max == Long.MAX_VALUE);
        }
        if (lower instanceof Float min && upper instanceof Float max) {
            return !(min == -Float.MAX_VALUE && max == Float.MAX_VALUE);
        }
        if (lower instanceof Double min && upper instanceof Double max) {
            return !(min == -Double.MAX_VALUE && max == Double.MAX_VALUE);
        }
        return true;
    }

    private static <T extends Number & Comparable<T>> boolean hasUsableSliderRange(T lower, T upper) {
        if (!hasExplicitFiniteBounds(lower, upper)) {
            return false;
        }
        double min = lower.doubleValue();
        double max = upper.doubleValue();
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            return false;
        }
        double span = max - min;
        if (!Double.isFinite(span) || span <= 0.0 || span > MAX_SLIDER_SPAN) {
            return false;
        }
        if (lower instanceof Integer minI && upper instanceof Integer maxI) {
            return minI > Integer.MIN_VALUE / 2 && maxI < Integer.MAX_VALUE / 2;
        }
        if (lower instanceof Long minL && upper instanceof Long maxL) {
            return minL > Long.MIN_VALUE / 2 && maxL < Long.MAX_VALUE / 2;
        }
        if (lower instanceof Float minF && upper instanceof Float maxF) {
            return minF > -Float.MAX_VALUE / 2.0f && maxF < Float.MAX_VALUE / 2.0f;
        }
        if (lower instanceof Double minD && upper instanceof Double maxD) {
            return minD > -Double.MAX_VALUE / 2.0 && maxD < Double.MAX_VALUE / 2.0;
        }
        return false;
    }

    private final class NumericSlider extends AbstractSliderButton {
        private NumericSlider(int x, int y, int width, int height, double initialRatio) {
            super(x, y, width, height, Component.empty(), Mth.clamp(initialRatio, 0.0, 1.0));
            this.updateMessage();
        }

        private void syncFromValue(T newValue) {
            this.value = NumericDetailPopup.this.computeNormalizedRatio(newValue);
            this.setMessage(Component.translatable("config_overhauled.word.value")
                    .append(Component.literal(": " + NumericDetailPopup.this.formatValue(newValue))));
        }

        @Override
        protected void updateMessage() {
            T mapped = NumericDetailPopup.this.mapRatioToValue(this.value);
            this.setMessage(Component.translatable("config_overhauled.word.value")
                    .append(Component.literal(": " + NumericDetailPopup.this.formatValue(mapped))));
        }

        @Override
        protected void applyValue() {
            if (NumericDetailPopup.this.isLocked) {
                return;
            }
            T mapped = NumericDetailPopup.this.mapRatioToValue(this.value);
            NumericDetailPopup.this.syncingSlider = true;
            try {
                NumericDetailPopup.this.setWorkingText(NumericDetailPopup.this.formatValue(mapped));
            } finally {
                NumericDetailPopup.this.syncingSlider = false;
            }
        }
    }
}
