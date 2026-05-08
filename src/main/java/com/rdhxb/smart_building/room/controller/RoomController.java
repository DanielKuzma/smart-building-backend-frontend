package com.rdhxb.smart_building.room.controller;

import com.rdhxb.smart_building.room.DTO.RoomRequest;
import com.rdhxb.smart_building.room.entity.Room;
import com.rdhxb.smart_building.room.service.RoomService;
import jakarta.persistence.GeneratedValue;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;


    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> getRooms(){
        return roomService.getRooms();
    }

    @GetMapping("/{id}")
    public Room getRoom(@PathVariable long id){
        return roomService.getRoom(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
    @Transactional
    public void addRoom(@RequestBody RoomRequest room){
        roomService.addRoom(room);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
    @Transactional
    public void deleteRoom(@PathVariable long id){
        roomService.deleteRoom(id);
    }
}
