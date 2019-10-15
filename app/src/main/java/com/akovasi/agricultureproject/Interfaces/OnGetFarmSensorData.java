package com.akovasi.agricultureproject.Interfaces;


import com.akovasi.agricultureproject.datatypes.farmdata.ModuleData;

import java.util.ArrayList;

public interface OnGetFarmSensorData {
    public void onFarmSensorData(ArrayList<ModuleData> moduleData);
}
