package de.keksuccino.snappy.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class UIFormatting {

    private UIFormatting() {
    }

    @NotNull
    public static Component optionMessage(@NotNull String key, @NotNull Component value) {
        return Component.translatable(key, value);
    }

    @NotNull
    public static Component optionMessage(@NotNull String key, @NotNull Object... args) {
        return Component.translatable(key, args);
    }

    @NotNull
    public static Component colored(@NotNull Component value, int color) {
        return value.copy().withStyle(Style.EMPTY.withColor(color));
    }

    @NotNull
    public static Component colored(@NotNull Component value, @NotNull ChatFormatting color) {
        return value.copy().withStyle(color);
    }

    @NotNull
    public static Component cycleValue(@NotNull Component value, int color) {
        return colored(value, color);
    }

    @NotNull
    public static Component enabledDisabledValue(boolean enabled, @NotNull String enabledKey, @NotNull String disabledKey) {
        return Component.translatable(enabled ? enabledKey : disabledKey)
                .withStyle(Style.EMPTY.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @NotNull
    public static Component visibleHiddenValue(boolean visible, @NotNull String visibleKey, @NotNull String hiddenKey) {
        return Component.translatable(visible ? visibleKey : hiddenKey)
                .withStyle(Style.EMPTY.withColor(visible ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @NotNull
    public static Component percentValue(@NotNull String key, double value, int color) {
        return Component.translatable(key, Math.round(value * 100.0D))
                .withStyle(Style.EMPTY.withColor(color));
    }

    @NotNull
    public static Component signedPercentValue(@NotNull String key, double value, int color) {
        int percent = (int) Math.round(value * 100.0D);
        String sign = percent > 0 ? "+" : "";
        return Component.translatable(key, sign + percent)
                .withStyle(Style.EMPTY.withColor(color));
    }

    @NotNull
    public static Component fixedLiteral(double value, @NotNull String pattern, int color) {
        return Component.literal(String.format(Locale.ROOT, pattern, value))
                .withStyle(Style.EMPTY.withColor(color));
    }

    @NotNull
    public static Component fixedTranslatable(@NotNull String key, double value, @NotNull String pattern, int color) {
        return Component.translatable(key, String.format(Locale.ROOT, pattern, value))
                .withStyle(Style.EMPTY.withColor(color));
    }

}
