package com.rdhxb.smart_building.room.repo;

import com.rdhxb.smart_building.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepo extends JpaRepository<Room, Long> {
    boolean existsByName(String name);

}
