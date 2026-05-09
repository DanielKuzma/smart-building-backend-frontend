package com.rdhxb.smart_building.device.entity;

import com.rdhxb.smart_building.room.entity.Room;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private DeviceType deviceType;
    private DeviceStatus deviceStatus;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private String properties;

}
