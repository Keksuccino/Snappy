package de.keksuccino.snappy.client.render.config;

public record DepthOfFieldConfig(boolean enabled, float focusDistance, float focalLength, float aperture) {

    public boolean active() {
        return this.enabled;
    }

}
