package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.TableAvailabilityResponse;
import com.example.htql_nhahang_khachsan.dto.TableResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.RestaurantTableEntity;
import com.example.htql_nhahang_khachsan.enums.TableStatus;
import com.example.htql_nhahang_khachsan.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;

//    public TableAvailabilityResponse getTableAvailability(Long branchId, Integer minCapacity) {
//        List<RestaurantTableEntity> tables;
//
//        if (minCapacity != null) {
//            tables = tableRepository.findByBranchIdAndCapacityGreaterThanEqualAndStatus(
//                    branchId, minCapacity, TableStatus.AVAILABLE
//            );
//        } else {
//            tables = tableRepository.findByBranchIdAndStatus(branchId, TableStatus.AVAILABLE);
//        }
//
//        BranchEntity branch = branchRepository.findById(branchId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi nhánh"));
//
//        return TableAvailabilityResponse.builder()
//                .branchId(branch.getId())
//                .branchName(branch.getName())
//                .checkTime(LocalDateTime.now())
//                .tables(tables.stream().map(TableResponse::fromEntity).toList())
//                .statistics(TableStatistics.fromTables(tables))
//                .build();
//    }


    public List<TableResponse> getTablesByBranch(Long branchId) {
        List<RestaurantTableEntity> tables = tableRepository.findByBranchIdOrderByTableNumber(branchId);

        return tables.stream().map(table -> {
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
        }).collect(Collectors.toList());
    }
}