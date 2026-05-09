package com.rdhxb.smart_building.device.service;

import com.rdhxb.smart_building.device.DTO.DeviceRequest;
import com.rdhxb.smart_building.device.entity.Device;
import com.rdhxb.smart_building.device.entity.DeviceStatus;
import com.rdhxb.smart_building.device.repo.DeviceRepo;
import com.rdhxb.smart_building.room.repo.RoomRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepo deviceRepo;
    private final RoomRepo roomRepo;

    public DeviceService(DeviceRepo deviceRepo, RoomRepo roomRepo) {
        this.deviceRepo = deviceRepo;
        this.roomRepo = roomRepo;
    }


    public List<Device> getDevices(){
        return deviceRepo.findAll();
    }

    public List<Device> getAllDevicesFromRoom(long roomId){
        return deviceRepo.findByRoom_Id(roomId);
    }

    public Device getDeviceById(long id){
        return deviceRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("No deivice with id: " +  id ));
    }

    public void addDeviceToRoomId(DeviceRequest device){
        Device device1 = new Device(
                null,
                device.getName(),
                device.getDeviceType(),
                device.getDeviceStatus(),
                roomRepo.findById(device.getRoomId()).orElseThrow(() -> new EntityNotFoundException("No room with id: " + device.getRoomId())),
                device.getProperties()

        );
        deviceRepo.save(device1);
    }

    public void deleteDevice(long id){
        deviceRepo.delete(deviceRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("No device with id: " + id)));
    }

    public void changeStatus(DeviceStatus deviceStatus, long id){
        Device d = getDeviceById(id);
        d.setDeviceStatus(deviceStatus);
        deviceRepo.save(d);
    }

}
