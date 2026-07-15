package de.keksuccino.snappy.util.rendering.gui.widget;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.gui.GuiBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Reusable button base with support for independently configurable background textures in each render state.
 */
public class SnappyButton extends Button {

    public static final Identifier DEFAULT_IDLE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/normal_20x20.png");
    public static final Identifier DEFAULT_HOVER_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/hover_20x20.png");
    public static final Identifier DEFAULT_DISABLED_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/disabled_20x20.png");

    private static final int BACKGROUND_TEXTURE_SIZE = 20;
    private static final int BACKGROUND_BORDER_SIZE = 4;

    @Nullable
    private Identifier idleBackgroundTexture = DEFAULT_IDLE_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier hoverBackgroundTexture = DEFAULT_HOVER_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier disabledBackgroundTexture = DEFAULT_DISABLED_BACKGROUND_TEXTURE;
    @Nullable
    private CreateNarration narration;
    private boolean useVanillaTextures = false;

    public SnappyButton(int x, int y, int width, int height, @NotNull Component message, @NotNull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /**
     * Sets 20x20 background textures for each render state. Each custom texture is nine-sliced with a 4-pixel border on every side. A null texture keeps Vanilla's default sprite for that state. The hover texture also applies while keyboard-focused, matching Vanilla behavior.
     */
    @NotNull
    public SnappyButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleBackgroundTexture = idleTexture;
        this.hoverBackgroundTexture = hoverTexture;
        this.disabledBackgroundTexture = disabledTexture;
        return this;
    }

    /**
     * Controls whether Vanilla button sprites are rendered instead of this button's custom background textures.
     */
    @NotNull
    public SnappyButton useVanillaTextures(boolean useVanillaTextures) {
        this.useVanillaTextures = useVanillaTextures;
        return this;
    }

    /**
     * Sets a custom narration factory, or restores the default narration when null.
     */
    @NotNull
    public SnappyButton setNarration(@Nullable CreateNarration narration) {
        this.narration = narration;
        return this;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        if (this.narration == null) {
            return super.createNarrationMessage();
        }
        return this.narration.createNarrationMessage(() -> super.createNarrationMessage());
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractBackground(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    protected final void extractBackground(@NotNull GuiGraphicsExtractor graphics) {
        if (this.useVanillaTextures) {
            this.extractDefaultSprite(graphics);
            return;
        }
        Identifier backgroundTexture = this.backgroundTexture();
        if (backgroundTexture == null) {
            this.extractDefaultSprite(graphics);
            return;
        }
        GuiBackground.render(graphics, backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, ARGB.white(this.alpha));
    }

    @Nullable
    private Identifier backgroundTexture() {
        if (!this.active) {
            return this.disabledBackgroundTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverBackgroundTexture : this.idleBackgroundTexture;
    }

}
