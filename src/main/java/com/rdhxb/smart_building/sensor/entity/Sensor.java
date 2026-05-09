package com.rdhxb.smart_building.sensor.entity;

import com.rdhxb.smart_building.device.entity.Device;
import com.rdhxb.smart_building.room.entity.Room;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sensors")
@Data
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private SensorType sensorType;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Setter
    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

//    "C", "%" ext
    private String unit;

    private boolean enabled;


}
