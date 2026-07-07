package de.keksuccino.snappy.client.render.config;

public record FilmEffectsConfig(float filmGrain, float chromaticAberration) {

    public boolean active() {
        return this.filmGrain > 1.0E-4F || this.chromaticAberration > 1.0E-4F;
    }

}
