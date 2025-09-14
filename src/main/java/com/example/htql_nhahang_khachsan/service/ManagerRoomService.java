package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.RoomRequest;
import com.example.htql_nhahang_khachsan.dto.RoomResponse;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.repository.RoomTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagerRoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BranchRepository branchRepository;


    public List<RoomResponse> getAllRoomsByBranch(Long branchId) {
        List<RoomEntity> rooms = roomRepository.findByRoomTypeBranchIdOrderByFloorAscRoomNumberAsc(branchId);
        return rooms.stream().map(this::toRoomResponse).collect(Collectors.toList());
    }

    public List<RoomResponse> searchRooms(Long branchId, String roomNumber, RoomStatus status,
                                          Long roomTypeId, Integer floor) {
        Specification<RoomEntity> spec = Specification.allOf();

        // Filter by branch
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("roomType").get("branch").get("id"), branchId));

        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("roomNumber")), "%" + roomNumber.toLowerCase() + "%"));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (roomTypeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("roomType").get("id"), roomTypeId));
        }

        if (floor != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("floor"), floor));
        }

        List<RoomEntity> rooms = roomRepository.findAll(spec,
                Sort.by(Sort.Direction.ASC, "floor", "roomNumber"));

        return rooms.stream().map(this::toRoomResponse).collect(Collectors.toList());
    }


    public RoomResponse getRoomById(Long roomId, Long branchId) {
        RoomEntity room = roomRepository.findByIdAndRoomTypeBranchId(roomId, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));
        return toRoomResponse(room);
    }

    public void createRoom(Long branchId, RoomRequest request) {
        // Validate room type belongs to branch
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndBranchId(request.getRoomTypeId(), branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng"));

        // Check duplicate room number in same branch
        if (roomRepository.existsByRoomNumberAndRoomTypeBranchId(request.getRoomNumber(), branchId)) {
            throw new IllegalArgumentException("Số phòng đã tồn tại trong chi nhánh này");
        }

        RoomEntity room = RoomEntity.builder()
                .roomType(roomType)
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();

        roomRepository.save(room);
    }

    public void updateRoom(Long roomId, Long branchId, RoomRequest request) {
        RoomEntity room = roomRepository.findByIdAndRoomTypeBranchId(roomId, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));

        // Validate room type belongs to branch
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndBranchId(request.getRoomTypeId(), branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng"));

        // Check duplicate room number (exclude current room)
        if (!room.getRoomNumber().equals(request.getRoomNumber()) &&
                roomRepository.existsByRoomNumberAndRoomTypeBranchId(request.getRoomNumber(), branchId)) {
            throw new IllegalArgumentException("Số phòng đã tồn tại trong chi nhánh này");
        }

        room.setRoomType(roomType);
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setStatus(request.getStatus());
        room.setNotes(request.getNotes());

        roomRepository.save(room);
    }

    public void deleteRoom(Long roomId, Long branchId) {
        RoomEntity room = roomRepository.findByIdAndRoomTypeBranchId(roomId, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));

        roomRepository.delete(room);
    }


    public void toggleRoomStatus(Long roomId, Long branchId) {
        RoomEntity room = roomRepository.findByIdAndRoomTypeBranchId(roomId, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));

        // Toggle between AVAILABLE and OUT_OF_ORDER (hidden)
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.OUT_OF_ORDER);
        } else if (room.getStatus() == RoomStatus.OUT_OF_ORDER) {
            room.setStatus(RoomStatus.AVAILABLE);
        }

        roomRepository.save(room);
    }

    private RoomResponse toRoomResponse(RoomEntity room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomTypeId(room.getRoomType().getId())
                .roomTypeName(room.getRoomType().getName())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus())
                .notes(room.getNotes())
                .lastCleaned(room.getLastCleaned())
                .createdAt(room.getCreatedAt())
                .build();
    }


}