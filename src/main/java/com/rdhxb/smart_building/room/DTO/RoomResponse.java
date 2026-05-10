package com.rdhxb.smart_building.room.DTO;

import com.rdhxb.smart_building.room.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private Long id;
    private String name;
    private String description;
    private int floor;
    private double areaInSquareM;

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getFloor(),
                room.getAreaInSquareM()
        );
    }
}