package in.project.main.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import in.project.main.services.EmpSalesInfoService;

@Controller
public class EmpSalesInfoController
{
	@Autowired
	private EmpSalesInfoService empSalesInfoService;
	
	@GetMapping("/sales")
	public String openSalesPage(Model model)
	{
		String totalSales = empSalesInfoService.findTotalSalesByAllEmployees();
		model.addAttribute("totalSales", totalSales);
		
		List<Object[]> salesList = empSalesInfoService.findTotalSalesByEachEmployee();
		model.addAttribute("salesList", salesList);
		
		return "sales";
	}
}
