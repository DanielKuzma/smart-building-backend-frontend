package com.rdhxb.smart_building.sensor.service;

import com.rdhxb.smart_building.sensor.entity.Sensor;
import com.rdhxb.smart_building.sensor.repo.SensorRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorService {

    private final SensorRepo sensorRepo;


    public SensorService(SensorRepo sensorRepo) {
        this.sensorRepo = sensorRepo;
    }

    public List<Sensor> getSensors(){
        return sensorRepo.findAll();
    }

    public List<Sensor> getAllSensorsInRoom(long roomId){
        return sensorRepo.findAllByRoom_Id(roomId);
    }

    public Sensor getSensor(long id){
        return sensorRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("No sensor with id: " + id));
    }

    public void addSensor(Sensor sensor){
        sensorRepo.save(sensor);
    }

    public void deleteSensor(long id){
        Sensor sensor = getSensor(id);
        sensorRepo.delete(sensor);
    }




}
