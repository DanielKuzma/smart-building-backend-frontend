package com.rdhxb.smart_building.sensor.entity;

import com.rdhxb.smart_building.common.AuditingEntity;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Table(name="sensor_readings")
public class SensorReading extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    private Double value;
    private String valueText;

    @Column(nullable = false)
    private Instant recordedAt;

}

