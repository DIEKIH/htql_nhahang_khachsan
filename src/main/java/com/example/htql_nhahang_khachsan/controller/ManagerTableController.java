package com.example.htql_nhahang_khachsan.controller;


import com.example.htql_nhahang_khachsan.dto.TableRequest;
import com.example.htql_nhahang_khachsan.dto.TableResponse;
import com.example.htql_nhahang_khachsan.enums.TableStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerTableService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;



import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerTableController {

    private final AuthService authService;
    private final ManagerTableService tableService;

    // === TABLE MANAGEMENT ===
    @GetMapping("/tables")
    public String tableList(Model model, HttpSession session,
                            @RequestParam(required = false) String tableNumber,
                            @RequestParam(required = false) TableStatus status,
                            @RequestParam(required = false) Integer capacity) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        List<TableResponse> tables;
        if (tableNumber != null || status != null || capacity != null) {
            tables = tableService.searchTables(branchId, tableNumber, status, capacity);
        } else {
            tables = tableService.getAllTablesByBranch(branchId);
        }

        // --- Đếm số lượng theo trạng thái ---
        long availableCount = tables.stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE)
                .count();

        long occupiedCount = tables.stream()
                .filter(t -> t.getStatus() == TableStatus.OCCUPIED)
                .count();

        long reservedCount = tables.stream()
                .filter(t -> t.getStatus() == TableStatus.RESERVED)
                .count();

        // --- Đưa vào model ---
        model.addAttribute("tables", tables);
        model.addAttribute("statuses", TableStatus.values());
        model.addAttribute("searchTableNumber", tableNumber);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchCapacity", capacity);

        model.addAttribute("availableCount", availableCount);
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("reservedCount", reservedCount);

        return "manager/tables/list";
    }


    @GetMapping("/tables/add")
    public String addTableForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        model.addAttribute("tableRequest", new TableRequest());
        model.addAttribute("statuses", TableStatus.values());

        return "manager/tables/form";
    }

    @PostMapping("/tables/add")
    public String addTable(@Valid @ModelAttribute TableRequest request,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("statuses", TableStatus.values());
            return "manager/tables/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            tableService.createTable(branchId, request);
            redirectAttributes.addFlashAttribute("success", "Thêm bàn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/tables";
    }

    @GetMapping("/tables/{id}/edit")
    public String editTableForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            TableResponse table = tableService.getTableById(id, branchId);

            TableRequest tableRequest = new TableRequest();
            tableRequest.setTableNumber(table.getTableNumber());
            tableRequest.setCapacity(table.getCapacity());
            tableRequest.setStatus(table.getStatus());
            tableRequest.setPositionX(table.getPositionX());
            tableRequest.setPositionY(table.getPositionY());
            tableRequest.setNotes(table.getNotes());

            model.addAttribute("tableRequest", tableRequest);
            model.addAttribute("tableId", id);
            model.addAttribute("statuses", TableStatus.values());

            return "manager/tables/form";
        } catch (Exception e) {
            return "redirect:/manager/tables";
        }
    }

    @PostMapping("/tables/{id}/edit")
    public String updateTable(@PathVariable Long id,
                              @Valid @ModelAttribute TableRequest request,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("tableId", id);
            model.addAttribute("statuses", TableStatus.values());
            return "manager/tables/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            tableService.updateTable(id, branchId, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật bàn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/tables";
    }

    @PostMapping("/tables/{id}/delete")
    public String deleteTable(@PathVariable Long id,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            tableService.deleteTable(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Xóa bàn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/tables";
    }

    @PostMapping("/tables/{id}/toggle-status")
    public String toggleTableStatus(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            tableService.toggleTableStatus(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái bàn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/tables";
    }
}
