package de.keksuccino.snappy.client.render.config;

import de.keksuccino.snappy.photo.PhotoModeStylizePreset;
import org.jetbrains.annotations.NotNull;

public record StylizeConfig(@NotNull PhotoModeStylizePreset preset) {

    public boolean active() {
        return this.preset.appliesShader();
    }

    public int shaderIndex() {
        return this.preset.shaderIndex();
    }

}
