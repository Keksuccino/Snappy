package de.keksuccino.snappy.client.render.config;

public record BloomConfig(float intensity) {

    public boolean active() {
        return this.intensity > 1.0E-4F;
    }

}
