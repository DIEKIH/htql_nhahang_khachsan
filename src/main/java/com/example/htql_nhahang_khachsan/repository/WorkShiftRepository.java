package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.WorkShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShiftEntity, Long> {

    List<WorkShiftEntity> findByWorkWeekId(Long workWeekId);

    Optional<WorkShiftEntity> findByWorkWeekIdAndDayOfWeek(Long workWeekId, DayOfWeek dayOfWeek);
}