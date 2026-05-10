package com.rdhxb.smart_building.sensor.repo;

import com.rdhxb.smart_building.sensor.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SensorRepo extends JpaRepository<Sensor,Long> {
    List<Sensor> findAllByRoom_Id(Long roomId);

}
