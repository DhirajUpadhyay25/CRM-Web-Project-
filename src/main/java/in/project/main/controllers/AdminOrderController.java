package in.project.main.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.project.main.entities.Orders;
import in.project.main.repositories.OrdersRepository;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrdersRepository ordersRepository;

    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Orders> orders;

        if (search != null && !search.trim().isEmpty()) {
            orders = ordersRepository.searchOrders(search.trim(), pageable);
            model.addAttribute("search", search.trim());
        } else {
            orders = ordersRepository.findAll(pageable);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("totalRevenue", ordersRepository.calculateTotalRevenue());
        model.addAttribute("totalOrders", ordersRepository.count());

        return "admin/commerce/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        Optional<Orders> order = ordersRepository.findById(id);
        if (order.isPresent()) {
            model.addAttribute("order", order.get());
        } else {
            model.addAttribute("errorMsg", "Order not found");
        }
        return "admin/commerce/orders/detail";
    }
}
