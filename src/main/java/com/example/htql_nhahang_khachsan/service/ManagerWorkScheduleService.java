package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.WorkWeekRequest;
import com.example.htql_nhahang_khachsan.dto.WorkWeekResponse;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.AttendanceStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftType;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerWorkScheduleService {

    private final WorkWeekRepository workWeekRepository;
    private final WorkShiftRepository workShiftRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public List<WorkWeekResponse> getWorkSchedulesByBranchAndDateRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        List<WorkWeekEntity> workWeeks = workWeekRepository.findByBranchAndDateRange(branchId, startDate, endDate);
        return workWeeks.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkWeekResponse createWorkWeek(Long branchId, Long managerId, WorkWeekRequest request) {
        // Validate staff
        UserEntity staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (staff.getRole() != UserRole.STAFF || !staff.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Nhân viên không hợp lệ");
        }

        // Validate branch
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Check duplicate
        Optional<WorkWeekEntity> existing = workWeekRepository.findByStaffIdAndWeekStart(
                request.getStaffId(), request.getWeekStart()
        );
        if (existing.isPresent()) {
            throw new RuntimeException("Nhân viên đã có lịch làm việc cho tuần này");
        }

        // Calculate week end (Sunday)
        LocalDate weekEnd = request.getWeekStart().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Create work week
        WorkWeekEntity workWeek = WorkWeekEntity.builder()
                .staff(staff)
                .branch(branch)
                .weekStart(request.getWeekStart())
                .weekEnd(weekEnd)
                .createdBy(managerId)
                .shifts(new ArrayList<>())
                .build();

        // Create shifts for each day
        if (request.getWeekSchedule() != null) {
            for (Map.Entry<DayOfWeek, ShiftType> entry : request.getWeekSchedule().entrySet()) {
                DayOfWeek day = entry.getKey();
                ShiftType type = entry.getValue();

                if (type == ShiftType.OFF) continue; // Skip OFF days

                WorkWeekRequest.TimeRange timeRange = request.getShiftTimes() != null
                        ? request.getShiftTimes().get(type)
                        : null;

                LocalTime startTime = timeRange != null ? timeRange.getStartTime()
                        : LocalTime.parse(type.getDefaultStart());
                LocalTime endTime = timeRange != null ? timeRange.getEndTime()
                        : LocalTime.parse(type.getDefaultEnd());

                WorkShiftEntity shift = WorkShiftEntity.builder()
                        .workWeek(workWeek)
                        .dayOfWeek(day)
                        .type(type)
                        .startTime(startTime)
                        .endTime(endTime)
                        .status(ShiftStatus.SCHEDULED)
                        .build();

                workWeek.getShifts().add(shift);
            }
        }

        WorkWeekEntity saved = workWeekRepository.save(workWeek);
        return convertToResponse(saved);
    }

    public WorkWeekResponse getWorkWeekById(Long id, Long branchId) {
        WorkWeekEntity workWeek = workWeekRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));

        if (!workWeek.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Lịch làm việc không thuộc chi nhánh này");
        }

        return convertToResponse(workWeek);
    }

    @Transactional
    public void deleteWorkWeek(Long id, Long branchId) {
        WorkWeekEntity workWeek = workWeekRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));

        if (!workWeek.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Lịch làm việc không thuộc chi nhánh này");
        }

        // Check if any shift has started
        boolean hasStarted = workWeek.getShifts().stream()
                .anyMatch(s -> s.getStatus() == ShiftStatus.COMPLETED);

        if (hasStarted) {
            throw new RuntimeException("Không thể xóa lịch đã có ca hoàn thành");
        }

        workWeekRepository.delete(workWeek);
    }

    @Transactional
    public void markAttendance(Long shiftId, Long branchId, boolean checkIn, LocalDateTime time, String notes) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        if (!shift.getWorkWeek().getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Ca làm việc không thuộc chi nhánh này");
        }

        Optional<AttendanceEntity> existingOpt = attendanceRepository.findByShiftId(shiftId);

        if (checkIn) {
            // Check in
            if (existingOpt.isPresent()) {
                throw new RuntimeException("Đã check-in trước đó");
            }

            AttendanceStatus status = determineAttendanceStatus(shift, time, true);

            AttendanceEntity attendance = AttendanceEntity.builder()
                    .shift(shift)
                    .staff(shift.getWorkWeek().getStaff())
                    .checkIn(time)
                    .status(status)
                    .notes(notes)
                    .build();

            attendanceRepository.save(attendance);
            shift.setStatus(ShiftStatus.STARTED);
            workShiftRepository.save(shift);
        } else {
            // Check out
            AttendanceEntity attendance = existingOpt
                    .orElseThrow(() -> new RuntimeException("Chưa check-in"));

            if (attendance.getCheckOut() != null) {
                throw new RuntimeException("Đã check-out trước đó");
            }

            attendance.setCheckOut(time);

            // Re-evaluate status based on check-out time
            AttendanceStatus finalStatus = determineAttendanceStatus(shift, time, false);
            if (attendance.getStatus() == AttendanceStatus.LATE || finalStatus == AttendanceStatus.LEFT_EARLY) {
                attendance.setStatus(finalStatus);
            }

            attendanceRepository.save(attendance);
            shift.setStatus(ShiftStatus.COMPLETED);
            workShiftRepository.save(shift);
        }
    }

    private AttendanceStatus determineAttendanceStatus(WorkShiftEntity shift, LocalDateTime actualTime, boolean isCheckIn) {
        LocalTime scheduledTime = isCheckIn ? shift.getStartTime() : shift.getEndTime();
        LocalTime actualLocalTime = actualTime.toLocalTime();

        if (isCheckIn) {
            // Late if more than 15 minutes late
            return actualLocalTime.isAfter(scheduledTime.plusMinutes(15))
                    ? AttendanceStatus.LATE
                    : AttendanceStatus.PRESENT;
        } else {
            // Left early if more than 15 minutes early
            return actualLocalTime.isBefore(scheduledTime.minusMinutes(15))
                    ? AttendanceStatus.LEFT_EARLY
                    : AttendanceStatus.PRESENT;
        }
    }

    public List<UserEntity> getStaffByBranch(Long branchId) {
        return userRepository.findByBranchIdAndRole(branchId, UserRole.STAFF);
    }

    private WorkWeekResponse convertToResponse(WorkWeekEntity workWeek) {
        UserEntity creator = workWeek.getCreatedBy() != null
                ? userRepository.findById(workWeek.getCreatedBy()).orElse(null)
                : null;

        List<WorkWeekResponse.ShiftResponse> shiftResponses = new ArrayList<>();
        Map<DayOfWeek, WorkWeekResponse.ShiftResponse> shiftsByDay = new HashMap<>();

        for (WorkShiftEntity shift : workWeek.getShifts()) {
            WorkWeekResponse.ShiftResponse shiftResponse = convertShiftToResponse(shift, workWeek.getWeekStart());
            shiftResponses.add(shiftResponse);
            shiftsByDay.put(shift.getDayOfWeek(), shiftResponse);
        }

        return WorkWeekResponse.builder()
                .id(workWeek.getId())
                .staffId(workWeek.getStaff().getId())
                .staffName(workWeek.getStaff().getFullName())
                .staffPhone(workWeek.getStaff().getPhoneNumber())
                .branchId(workWeek.getBranch().getId())
                .branchName(workWeek.getBranch().getName())
                .weekStart(workWeek.getWeekStart())
                .weekEnd(workWeek.getWeekEnd())
                .formattedWeekRange(workWeek.getWeekStart().format(DATE_FORMATTER) + " - " + workWeek.getWeekEnd().format(DATE_FORMATTER))
                .shifts(shiftResponses)
                .shiftsByDay(shiftsByDay)
                .createdAt(workWeek.getCreatedAt())
                .createdByName(creator != null ? creator.getFullName() : "N/A")
                .build();
    }

//    private WorkWeekResponse.ShiftResponse convertShiftToResponse(WorkShiftEntity shift, LocalDate weekStart) {
//        // Calculate actual date
//        LocalDate date = weekStart.with(TemporalAdjusters.nextOrSame(shift.getDayOfWeek()));
//
//        // Get attendance if exists
//        Optional<AttendanceEntity> attendanceOpt = attendanceRepository.findByShiftId(shift.getId());
//        WorkWeekResponse.AttendanceInfo attendanceInfo = null;
//
//        if (attendanceOpt.isPresent()) {
//            AttendanceEntity att = attendanceOpt.get();
//            attendanceInfo = WorkWeekResponse.AttendanceInfo.builder()
//                    .id(att.getId())
//                    .checkIn(att.getCheckIn())
//                    .checkOut(att.getCheckOut())
//                    .formattedCheckIn(att.getCheckIn() != null ? att.getCheckIn().format(DATETIME_FORMATTER) : null)
//                    .formattedCheckOut(att.getCheckOut() != null ? att.getCheckOut().format(DATETIME_FORMATTER) : null)
//                    .attendanceStatus(att.getStatus().name())
//                    .attendanceStatusDisplay(att.getStatus().getDisplayName())
//                    .attendanceStatusBadge(att.getStatus().getBadgeClass())
//                    .isLate(att.getStatus() == AttendanceStatus.LATE)
//                    .isLeftEarly(att.getStatus() == AttendanceStatus.LEFT_EARLY)
//                    .notes(att.getNotes())
//                    .build();
//        }
//
//        return WorkWeekResponse.ShiftResponse.builder()
//                .id(shift.getId())
//                .dayOfWeek(shift.getDayOfWeek())
//                .dayDisplayName(getDayDisplayName(shift.getDayOfWeek()))
//                .date(date)
//                .formattedDate(date.format(DATE_FORMATTER))
//                .type(shift.getType())
//                .typeDisplayName(shift.getType().getDisplayName())
//                .typeBadgeClass(shift.getType().getBadgeClass())
//                .startTime(shift.getStartTime())
//                .endTime(shift.getEndTime())
//                .formattedTime(shift.getStartTime().format(TIME_FORMATTER) + " - " + shift.getEndTime().format(TIME_FORMATTER))
//                .status(shift.getStatus())
//                .statusDisplayName(shift.getStatus().getDisplayName())
//                .statusBadgeClass(shift.getStatus().getBadgeClass())
//                .attendance(attendanceInfo)
//                .notes(shift.getNotes())
//                .build();
//    }

    private WorkWeekResponse.ShiftResponse convertShiftToResponse(WorkShiftEntity shift, LocalDate weekStart) {
        // ✅ Fix lệch ngày
        LocalDate date = weekStart.plusDays(shift.getDayOfWeek().getValue() - 1);

        // Get attendance if exists
        Optional<AttendanceEntity> attendanceOpt = attendanceRepository.findByShiftId(shift.getId());
        WorkWeekResponse.AttendanceInfo attendanceInfo = null;
        if (attendanceOpt.isPresent()) {
            AttendanceEntity att = attendanceOpt.get();
            attendanceInfo = WorkWeekResponse.AttendanceInfo.builder()
                    .id(att.getId())
                    .checkIn(att.getCheckIn())
                    .checkOut(att.getCheckOut())
                    .formattedCheckIn(att.getCheckIn() != null ? att.getCheckIn().format(DATETIME_FORMATTER) : null)
                    .formattedCheckOut(att.getCheckOut() != null ? att.getCheckOut().format(DATETIME_FORMATTER) : null)
                    .attendanceStatus(att.getStatus().name())
                    .attendanceStatusDisplay(att.getStatus().getDisplayName())
                    .attendanceStatusBadge(att.getStatus().getBadgeClass())
                    .isLate(att.getStatus() == AttendanceStatus.LATE)
                    .isLeftEarly(att.getStatus() == AttendanceStatus.LEFT_EARLY)
                    .notes(att.getNotes())
                    .build();
        }

        return WorkWeekResponse.ShiftResponse.builder()
                .id(shift.getId())
                .dayOfWeek(shift.getDayOfWeek())
                .dayDisplayName(getDayDisplayName(shift.getDayOfWeek()))
                .date(date)
                .formattedDate(date.format(DATE_FORMATTER))
                .type(shift.getType())
                .typeDisplayName(shift.getType().getDisplayName())
                .typeBadgeClass(shift.getType().getBadgeClass())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .formattedTime(shift.getStartTime().format(TIME_FORMATTER) + " - " + shift.getEndTime().format(TIME_FORMATTER))
                .status(shift.getStatus())
                .statusDisplayName(shift.getStatus().getDisplayName())
                .statusBadgeClass(shift.getStatus().getBadgeClass())
                .attendance(attendanceInfo)
                .notes(shift.getNotes())
                .build();
    }


    private String getDayDisplayName(DayOfWeek day) {
        Map<DayOfWeek, String> names = Map.of(
                DayOfWeek.MONDAY, "Thứ 2",
                DayOfWeek.TUESDAY, "Thứ 3",
                DayOfWeek.WEDNESDAY, "Thứ 4",
                DayOfWeek.THURSDAY, "Thứ 5",
                DayOfWeek.FRIDAY, "Thứ 6",
                DayOfWeek.SATURDAY, "Thứ 7",
                DayOfWeek.SUNDAY, "Chủ nhật"
        );
        return names.get(day);
    }
}