package com.example.htql_nhahang_khachsan.service;



import com.example.htql_nhahang_khachsan.dto.TableRequest;
import com.example.htql_nhahang_khachsan.dto.TableResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.RestaurantTableEntity;
import com.example.htql_nhahang_khachsan.enums.TableStatus;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.RestaurantTableRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerTableService {

    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;

    public List<TableResponse> getAllTablesByBranch(Long branchId) {
        List<RestaurantTableEntity> tables = tableRepository.findByBranchIdOrderByTableNumberAsc(branchId);
        return tables.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TableResponse> searchTables(Long branchId, String tableNumber, TableStatus status, Integer capacity) {
        List<RestaurantTableEntity> tables = tableRepository.searchTables(branchId, tableNumber, status, capacity);
        return tables.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TableResponse getTableById(Long id, Long branchId) {
        RestaurantTableEntity table = tableRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));
        return convertToResponse(table);
    }

    @Transactional
    public TableResponse createTable(Long branchId, TableRequest request) {
        // Kiểm tra branch có tồn tại không
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi nhánh với ID: " + branchId));

        // Kiểm tra số bàn đã tồn tại chưa
        if (tableRepository.existsByTableNumberAndBranchId(request.getTableNumber(), branchId)) {
            throw new IllegalArgumentException("Số bàn " + request.getTableNumber() + " đã tồn tại trong chi nhánh này");
        }

        RestaurantTableEntity table = RestaurantTableEntity.builder()
                .branch(branch)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .status(request.getStatus())
                .positionX(request.getPositionX())
                .positionY(request.getPositionY())
                .notes(request.getNotes())
                .build();

        RestaurantTableEntity savedTable = tableRepository.save(table);
        return convertToResponse(savedTable);
    }

    @Transactional
    public TableResponse updateTable(Long id, Long branchId, TableRequest request) {
        RestaurantTableEntity table = tableRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));

        // Kiểm tra số bàn đã tồn tại chưa (trừ bàn hiện tại)
        if (tableRepository.existsByTableNumberAndBranchIdAndIdNot(request.getTableNumber(), branchId, id)) {
            throw new IllegalArgumentException("Số bàn " + request.getTableNumber() + " đã tồn tại trong chi nhánh này");
        }

        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setStatus(request.getStatus());
        table.setPositionX(request.getPositionX());
        table.setPositionY(request.getPositionY());
        table.setNotes(request.getNotes());

        RestaurantTableEntity updatedTable = tableRepository.save(table);
        return convertToResponse(updatedTable);
    }

    @Transactional
    public void deleteTable(Long id, Long branchId) {
        RestaurantTableEntity table = tableRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));

        // Kiểm tra bàn có đang được sử dụng không
        if (table.getStatus() == TableStatus.OCCUPIED || table.getStatus() == TableStatus.RESERVED) {
            throw new IllegalArgumentException("Không thể xóa bàn đang được sử dụng hoặc đã được đặt");
        }

        tableRepository.delete(table);
    }

    @Transactional
    public void toggleTableStatus(Long id, Long branchId) {
        RestaurantTableEntity table = tableRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));

        TableStatus newStatus;
        if (table.getStatus() == TableStatus.OUT_OF_SERVICE) {
            newStatus = TableStatus.AVAILABLE;
        } else {
            newStatus = TableStatus.OUT_OF_SERVICE;
        }

        table.setStatus(newStatus);
        tableRepository.save(table);
    }

    private TableResponse convertToResponse(RestaurantTableEntity table) {
        return TableResponse.builder()
                .id(table.getId())
                .branchId(table.getBranch().getId())
                .branchName(table.getBranch().getName())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .positionX(table.getPositionX())
                .positionY(table.getPositionY())
                .notes(table.getNotes())
                .createdAt(table.getCreatedAt())
                .build();
    }
}
