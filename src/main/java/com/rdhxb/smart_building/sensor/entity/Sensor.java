package com.rdhxb.smart_building.sensor.entity;

import com.rdhxb.smart_building.common.AuditingEntity;
import com.rdhxb.smart_building.device.entity.Device;
import com.rdhxb.smart_building.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sensors")
@EqualsAndHashCode(callSuper = false)
@Data
public class Sensor extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
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
