package de.keksuccino.snappy;

import de.keksuccino.konkrete.config.Config;
import de.keksuccino.snappy.util.AbstractOptions;

public class InstanceData extends AbstractOptions {

    protected final Config instanceData = new Config(Snappy.SNAPSHOTS_DIR.getAbsolutePath().replace("\\", "/") + "/snappy_instance_data.txt");

    public final Option<Boolean> photoModeGridState = new Option<>(instanceData, "photo_mode_grid_state", true, "photo_mode");

    public InstanceData() {
        this.instanceData.syncConfig();
        this.instanceData.clearUnusedValues();
    }

}
