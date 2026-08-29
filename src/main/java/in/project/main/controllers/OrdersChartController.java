package in.project.main.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import in.project.main.services.OrdersChartService;

@Controller
public class OrdersChartController 
{
	@Autowired
	private OrdersChartService ordersChartService;
	
	@GetMapping("/admin/orders-chart")
	public String openOrdersChartPage(Model model)
	{
		return "admin/dashboard";
	}
}
