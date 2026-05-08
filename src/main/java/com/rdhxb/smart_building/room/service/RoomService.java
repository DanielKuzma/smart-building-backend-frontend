package com.rdhxb.smart_building.room.service;

import com.rdhxb.smart_building.room.DTO.RoomRequest;
import com.rdhxb.smart_building.room.entity.Room;
import com.rdhxb.smart_building.room.repo.RoomRepo;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RoomService {

    private final RoomRepo roomRepo;

    public RoomService(RoomRepo roomRepo) {
        this.roomRepo = roomRepo;
    }

//    get all rooms
    public List<Room> getRooms(){
        return roomRepo.findAll();
    }

//    get one room
    public Room getRoom(long id){
        return roomRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("No room with id: " + id));
    }

//    add new room
    public void addRoom(RoomRequest room) {
        if (roomRepo.existsByName(room.getName())) {
            throw new EntityExistsException(
                    "Room with name '" + room.getName() + "' already exists"
            );
        }
        Room newRoom = new Room(
                null,
                room.getName(),
                room.getDescription(),
                room.getFloor(),
                room.getAreaInSquareM()
        );
        roomRepo.save(newRoom);
    }
    
//    delete room 
    public boolean deleteRoom(long id){
        Room room = roomRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("No room with id: " + id));
        roomRepo.delete(room);
        return true;
    }


}
