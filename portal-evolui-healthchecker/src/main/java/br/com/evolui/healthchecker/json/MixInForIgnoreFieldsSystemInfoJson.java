package br.com.evolui.healthchecker.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oshi.hardware.*;
import oshi.software.os.OSProcess;

import java.util.List;

public abstract class MixInForIgnoreFieldsSystemInfoJson {
    @JsonIgnore
    abstract List<OSProcess> getProcesses(); // we don't need it!
    @JsonIgnore
    abstract List<PowerSource> getPowerSources();
    @JsonIgnore
    abstract List<Display> getDisplays();
    abstract Sensors getSensors();
    @JsonIgnore
    abstract List<SoundCard> getSoundCards();
    @JsonIgnore
    abstract List<GraphicsCard> getGraphicsCards();
    @JsonIgnore
    abstract List<UsbDevice> getUsbDevices(boolean var1);
}
