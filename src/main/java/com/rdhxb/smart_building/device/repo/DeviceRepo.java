package com.rdhxb.smart_building.device.repo;

import com.rdhxb.smart_building.device.entity.Device;
import com.rdhxb.smart_building.device.entity.DeviceStatus;
import com.rdhxb.smart_building.device.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepo extends JpaRepository<Device, Long> {

    public List<Device> findByRoom_Id(long id);
    public Device findByDeviceType(DeviceType deviceType);
    public List<Device> findByDeviceStatus(DeviceStatus deviceStatus);
}
