package de.keksuccino.snappy.client.render.config;

public record ColorAdjustmentConfig(float saturation, float contrast, float overexposure, float gamma) {

    public boolean active() {
        return Math.abs(this.gamma) > 1.0E-4F
                || Math.abs(this.saturation) > 1.0E-4F
                || Math.abs(this.contrast) > 1.0E-4F
                || Math.abs(this.overexposure) > 1.0E-4F;
    }

}
