package com.rdhxb.smart_building.room.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomRequest {
    private String name;
    private String description;
    private int floor;
    private double areaInSquareM;
}
