package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.ProfileUpdateRequest;
import com.example.htql_nhahang_khachsan.dto.WorkWeekResponse;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.AttendanceStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftStatus;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffWorkScheduleService {

    private final WorkWeekRepository workWeekRepository;
    private final WorkShiftRepository workShiftRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Lấy lịch làm việc của nhân viên theo khoảng thời gian
     */
    public List<WorkWeekResponse> getWorkSchedules(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<WorkWeekEntity> workWeeks = workWeekRepository.findByStaffAndDateRange(staffId, startDate, endDate);
        return workWeeks.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * Lấy ca làm việc cho một ngày cụ thể
     */
    public List<WorkWeekResponse.ShiftResponse> getShiftsForDate(Long staffId, LocalDate date) {
        LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WorkWeekEntity> workWeeks = workWeekRepository.findByStaffAndDateRange(staffId, weekStart, weekEnd);

        List<WorkWeekResponse.ShiftResponse> shifts = new ArrayList<>();
        for (WorkWeekEntity workWeek : workWeeks) {
            for (WorkShiftEntity shift : workWeek.getShifts()) {
                LocalDate shiftDate = weekStart.plusDays(shift.getDayOfWeek().getValue() - 1);
                if (shiftDate.equals(date)) {
                    shifts.add(convertShiftToResponse(shift, weekStart));
                }
            }
        }

        return shifts;
    }

    /**
     * Thống kê tuần
     */
    public Map<String, Object> getWeeklyStats(Long staffId, LocalDate weekStart, LocalDate weekEnd) {
        List<WorkWeekEntity> workWeeks = workWeekRepository.findByStaffAndDateRange(staffId, weekStart, weekEnd);

        int totalShifts = 0;
        int completedShifts = 0;
        int lateCount = 0;
        int presentCount = 0;

        for (WorkWeekEntity workWeek : workWeeks) {
            totalShifts += workWeek.getShifts().size();

            for (WorkShiftEntity shift : workWeek.getShifts()) {
                if (shift.getStatus() == ShiftStatus.COMPLETED) {
                    completedShifts++;
                }

                Optional<AttendanceEntity> attendanceOpt = attendanceRepository.findByShiftId(shift.getId());
                if (attendanceOpt.isPresent()) {
                    AttendanceEntity attendance = attendanceOpt.get();
                    if (attendance.getStatus() == AttendanceStatus.LATE) {
                        lateCount++;
                    } else if (attendance.getStatus() == AttendanceStatus.PRESENT) {
                        presentCount++;
                    }
                }
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShifts", totalShifts);
        stats.put("completedShifts", completedShifts);
        stats.put("upcomingShifts", totalShifts - completedShifts);
        stats.put("lateCount", lateCount);
        stats.put("presentCount", presentCount);
        stats.put("weekStart", weekStart.format(DATE_FORMATTER));
        stats.put("weekEnd", weekEnd.format(DATE_FORMATTER));

        return stats;
    }

    /**
     * Check-in ca làm việc (cho nhân viên tự check-in)
     */
    @Transactional
    public void checkIn(Long shiftId, Long staffId, LocalDateTime time, String notes) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        // Kiểm tra ca này có phải của nhân viên không
        if (!shift.getWorkWeek().getStaff().getId().equals(staffId)) {
            throw new RuntimeException("Ca làm việc không thuộc về bạn");
        }

        // ✅ KIỂM TRA THỜI GIAN: Check-in được trước giờ ca 30 phút
        LocalDate shiftDate = shift.getWorkWeek().getWeekStart().plusDays(shift.getDayOfWeek().getValue() - 1);
        LocalDateTime shiftStartDateTime = LocalDateTime.of(shiftDate, shift.getStartTime());
        LocalDateTime earliestCheckIn = shiftStartDateTime.minusMinutes(30);

        if (time.isBefore(earliestCheckIn)) {
            throw new RuntimeException("Chỉ được check-in trước giờ ca tối đa 30 phút. Giờ ca: " +
                    shift.getStartTime().format(TIME_FORMATTER));
        }

        // Kiểm tra đã check-in chưa
        Optional<AttendanceEntity> existingOpt = attendanceRepository.findByShiftId(shiftId);
        if (existingOpt.isPresent()) {
            throw new RuntimeException("Đã check-in ca này rồi");
        }

        // Xác định trạng thái (đúng giờ hay muộn)
        AttendanceStatus status = determineCheckInStatus(shift, time);

        AttendanceEntity attendance = AttendanceEntity.builder()
                .shift(shift)
                .staff(shift.getWorkWeek().getStaff())
                .checkIn(time)
                .status(status)
                .notes(notes)
                .build();

        attendanceRepository.save(attendance);

        // Cập nhật trạng thái ca
        shift.setStatus(ShiftStatus.STARTED);
        workShiftRepository.save(shift);
    }

    /**
     * Check-out ca làm việc (cho nhân viên tự check-out)
     */
    @Transactional
    public void checkOut(Long shiftId, Long staffId, LocalDateTime time, String notes) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        // Kiểm tra ca này có phải của nhân viên không
        if (!shift.getWorkWeek().getStaff().getId().equals(staffId)) {
            throw new RuntimeException("Ca làm việc không thuộc về bạn");
        }

        // ✅ KIỂM TRA THỜI GIAN: Check-out được sau giờ ca 2 tiếng
        LocalDate shiftDate = shift.getWorkWeek().getWeekStart().plusDays(shift.getDayOfWeek().getValue() - 1);
        LocalDateTime shiftEndDateTime = LocalDateTime.of(shiftDate, shift.getEndTime());
        LocalDateTime latestCheckOut = shiftEndDateTime.plusHours(2);

        if (time.isAfter(latestCheckOut)) {
            throw new RuntimeException("Chỉ được check-out sau giờ ca tối đa 2 tiếng. Giờ ca: " +
                    shift.getEndTime().format(TIME_FORMATTER));
        }

        // Kiểm tra đã check-in chưa
        AttendanceEntity attendance = attendanceRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new RuntimeException("Chưa check-in ca này"));

        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("Đã check-out ca này rồi");
        }

        attendance.setCheckOut(time);

        // Cập nhật trạng thái nếu về sớm
        AttendanceStatus checkOutStatus = determineCheckOutStatus(shift, time);
        if (checkOutStatus == AttendanceStatus.LEFT_EARLY) {
            attendance.setStatus(AttendanceStatus.LEFT_EARLY);
        }

        if (notes != null && !notes.trim().isEmpty()) {
            attendance.setNotes(attendance.getNotes() != null
                    ? attendance.getNotes() + " | " + notes
                    : notes);
        }

        attendanceRepository.save(attendance);

        // Cập nhật trạng thái ca
        shift.setStatus(ShiftStatus.COMPLETED);
        workShiftRepository.save(shift);
    }

    /**
     * Lấy lịch sử chấm công
     */
    public List<WorkWeekResponse.AttendanceInfo> getAttendanceHistory(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<AttendanceEntity> attendances = attendanceRepository.findByStaffAndDateRange(staffId, startDate, endDate);

        return attendances.stream()
                .map(this::convertAttendanceToInfo)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin cá nhân
     */
    @Transactional
    public void updateProfile(Long staffId, ProfileUpdateRequest request) {
        UserEntity staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        // Cập nhật thông tin cơ bản
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            staff.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            staff.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            staff.setAddress(request.getAddress().trim());
        }

        // Cập nhật email (kiểm tra trùng)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(staff.getEmail())) {
                Optional<UserEntity> existing = userRepository.findByEmailAndIdNot(newEmail, staffId);
                if (existing.isPresent()) {
                    throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");
                }
                staff.setEmail(newEmail);
            }
        }

        // Đổi mật khẩu (nếu có)
        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            }

            if (request.getNewPassword().length() < 6) {
                throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(staff);
    }

    // ===================== MANAGER CHẤM CÔNG THAY =====================

    /**
     * Manager check-in cho nhân viên (không giới hạn thời gian)
     */
    @Transactional
    public void managerCheckIn(Long shiftId, Long managerId, Long branchId, LocalDateTime time, String notes) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        // Kiểm tra ca có thuộc chi nhánh của manager không
        if (!shift.getWorkWeek().getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Ca làm việc không thuộc chi nhánh của bạn");
        }

        // Kiểm tra đã check-in chưa
        Optional<AttendanceEntity> existingOpt = attendanceRepository.findByShiftId(shiftId);
        if (existingOpt.isPresent()) {
            throw new RuntimeException("Nhân viên đã check-in ca này rồi");
        }

        // Xác định trạng thái (đúng giờ hay muộn)
        AttendanceStatus status = determineCheckInStatus(shift, time);

        AttendanceEntity attendance = AttendanceEntity.builder()
                .shift(shift)
                .staff(shift.getWorkWeek().getStaff())
                .checkIn(time)
                .status(status)
                .notes(notes != null ? "[Manager] " + notes : "[Manager chấm công thay]")
                .build();

        attendanceRepository.save(attendance);

        // Cập nhật trạng thái ca
        shift.setStatus(ShiftStatus.STARTED);
        workShiftRepository.save(shift);
    }

    /**
     * Manager check-out cho nhân viên (không giới hạn thời gian)
     */
    @Transactional
    public void managerCheckOut(Long shiftId, Long managerId, Long branchId, LocalDateTime time, String notes) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        // Kiểm tra ca có thuộc chi nhánh của manager không
        if (!shift.getWorkWeek().getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Ca làm việc không thuộc chi nhánh của bạn");
        }

        // Kiểm tra đã check-in chưa
        AttendanceEntity attendance = attendanceRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new RuntimeException("Chưa check-in ca này"));

        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("Đã check-out ca này rồi");
        }

        attendance.setCheckOut(time);

        // Cập nhật trạng thái nếu về sớm
        AttendanceStatus checkOutStatus = determineCheckOutStatus(shift, time);
        if (checkOutStatus == AttendanceStatus.LEFT_EARLY) {
            attendance.setStatus(AttendanceStatus.LEFT_EARLY);
        }

        String managerNote = "[Manager chấm công thay]" + (notes != null ? " - " + notes : "");
        attendance.setNotes(attendance.getNotes() != null
                ? attendance.getNotes() + " | " + managerNote
                : managerNote);

        attendanceRepository.save(attendance);

        // Cập nhật trạng thái ca
        shift.setStatus(ShiftStatus.COMPLETED);
        workShiftRepository.save(shift);
    }

    /**
     * Manager xóa chấm công (để chấm lại)
     */
    @Transactional
    public void managerDeleteAttendance(Long shiftId, Long managerId, Long branchId) {
        WorkShiftEntity shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        // Kiểm tra ca có thuộc chi nhánh của manager không
        if (!shift.getWorkWeek().getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Ca làm việc không thuộc chi nhánh của bạn");
        }

        Optional<AttendanceEntity> attendanceOpt = attendanceRepository.findByShiftId(shiftId);
        if (attendanceOpt.isPresent()) {
            attendanceRepository.delete(attendanceOpt.get());
            shift.setStatus(ShiftStatus.SCHEDULED);
            workShiftRepository.save(shift);
        } else {
            throw new RuntimeException("Không có dữ liệu chấm công để xóa");
        }
    }

    // Helper methods
    private AttendanceStatus determineCheckInStatus(WorkShiftEntity shift, LocalDateTime actualTime) {
        LocalTime scheduledTime = shift.getStartTime();
        LocalTime actualLocalTime = actualTime.toLocalTime();

        // Muộn hơn 15 phút = LATE
        return actualLocalTime.isAfter(scheduledTime.plusMinutes(15))
                ? AttendanceStatus.LATE
                : AttendanceStatus.PRESENT;
    }

    private AttendanceStatus determineCheckOutStatus(WorkShiftEntity shift, LocalDateTime actualTime) {
        LocalTime scheduledTime = shift.getEndTime();
        LocalTime actualLocalTime = actualTime.toLocalTime();

        // Về sớm hơn 15 phút = LEFT_EARLY
        return actualLocalTime.isBefore(scheduledTime.minusMinutes(15))
                ? AttendanceStatus.LEFT_EARLY
                : AttendanceStatus.PRESENT;
    }

    private WorkWeekResponse convertToResponse(WorkWeekEntity workWeek) {
        List<WorkWeekResponse.ShiftResponse> shiftResponses = new ArrayList<>();
        Map<java.time.DayOfWeek, WorkWeekResponse.ShiftResponse> shiftsByDay = new HashMap<>();

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
                .build();
    }

    private WorkWeekResponse.ShiftResponse convertShiftToResponse(WorkShiftEntity shift, LocalDate weekStart) {
        // ✅ Fix: Chuẩn hóa tính ngày
        LocalDate date = weekStart.plusDays(shift.getDayOfWeek().getValue() - 1);

        Optional<AttendanceEntity> attendanceOpt = attendanceRepository.findByShiftId(shift.getId());
        WorkWeekResponse.AttendanceInfo attendanceInfo = null;

        if (attendanceOpt.isPresent()) {
            attendanceInfo = convertAttendanceToInfo(attendanceOpt.get());
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

    private WorkWeekResponse.AttendanceInfo convertAttendanceToInfo(AttendanceEntity att) {
        WorkShiftEntity shift = att.getShift();
        return WorkWeekResponse.AttendanceInfo.builder()
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
                // ✅ Thêm 2 dòng này:
                .typeDisplayName(shift.getType().getDisplayName())
                .typeBadgeClass(shift.getType().getBadgeClass())
                .scheduledTime(shift.getStartTime().toString() + " - " + shift.getEndTime().toString())


                .build();
    }

    private String getDayDisplayName(java.time.DayOfWeek day) {
        Map<java.time.DayOfWeek, String> names = Map.of(
                java.time.DayOfWeek.MONDAY, "Thứ 2",
                java.time.DayOfWeek.TUESDAY, "Thứ 3",
                java.time.DayOfWeek.WEDNESDAY, "Thứ 4",
                java.time.DayOfWeek.THURSDAY, "Thứ 5",
                java.time.DayOfWeek.FRIDAY, "Thứ 6",
                java.time.DayOfWeek.SATURDAY, "Thứ 7",
                java.time.DayOfWeek.SUNDAY, "Chủ nhật"
        );
        return names.get(day);
    }
}