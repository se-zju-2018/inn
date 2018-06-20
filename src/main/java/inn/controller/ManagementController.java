package inn.controller;

import inn.model.Staff;
import inn.service.ReservationService;
import inn.service.UserService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ManagementController {

    private UserService userService;
    private ReservationService reservationService;

    @Autowired
    public ManagementController(UserService userService, ReservationService reservationService) {
        this.userService = userService;
        this.reservationService = reservationService;
    }

    @GetMapping("/management")
    public String showReservations(Model model) {
        val reservations = reservationService.findAllReservations();
        model.addAttribute("reservations", reservations);
        model.addAttribute("formatter", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "management";
    }

    @GetMapping("/checkin")
    public String showCheckin(Model model) {
        val reservations = reservationService.checkinReservations(LocalDate.now());
        model.addAttribute("reservations", reservations);
        return "checkin";
    }

    @GetMapping("/checkout")
    public String showCheckout(Model model) {
        val reservations = reservationService.checkoutReservations(LocalDate.now());
        model.addAttribute("reservations", reservations);
        return "checkout";
    }

    @PostMapping("/reservations/{id}")
    public String updateReservation(RedirectAttributes redirectAttributes, HttpSession session, @PathVariable int id,
                                    @RequestParam String step, @RequestParam int allocatedRoom) {
        val reservation = reservationService.findReservationById(id).get();
        val staff = (Staff) userService.findById((Integer) session.getAttribute("id")).get();
        if (step.equals("checkin")) {
            val room = reservationService.findRoomByNumber(allocatedRoom).get();
            reservation.setAllocatedRoom(room);
            reservation.setCheckinStaff(staff);
            reservation.setCheckinTime(LocalDateTime.now());
            reservationService.saveReservation(reservation);
            redirectAttributes.addFlashAttribute("message", "入住登记成功！");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/checkin";
        } else if (step.equals("checkout")) {
            reservation.setCheckoutStaff(staff);
            reservation.setCheckoutTime(LocalDateTime.now());
            reservationService.saveReservation(reservation);
            redirectAttributes.addFlashAttribute("message", "退房结账成功！");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/checkout";
        }
        return "redirect:/management";
    }

    @DeleteMapping("/reservations/{id}")
    public String deleteReservation(RedirectAttributes redirectAttributes, @PathVariable int id) {
        reservationService.deleteReservationById(id);
        redirectAttributes.addFlashAttribute("message", "预订记录删除成功！");
        redirectAttributes.addFlashAttribute("alertClass", "success");
        return "redirect:/management";
    }

    @GetMapping("/vacant/{reservationId}") @ResponseBody
    public String getVacantRooms(@PathVariable int reservationId) {
        val reservation = reservationService.findReservationById(reservationId).get();
        val rooms = reservationService.vacantRooms(reservation.getStartDate(), reservation.getRoomType());
        val response = new StringBuilder();
        for (val room : rooms) {
            response.append("<option>");
            response.append(room.getRoomNumber());
            response.append("</option>\n");
        }
        return response.toString();
    }

}
