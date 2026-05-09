package com.rdhxb.smart_building.device.DTO;

import com.rdhxb.smart_building.device.entity.DeviceStatus;
import com.rdhxb.smart_building.device.entity.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {
    private String name;
    private DeviceType deviceType;
    private long roomId;
    private DeviceStatus deviceStatus;
    private String properties;

}
