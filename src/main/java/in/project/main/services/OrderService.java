package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.entities.EmployeeOrders;
import in.project.main.entities.Orders;
import in.project.main.repositories.EmployeeOrdersRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.util.DateTimeUtil;

@Service
public class OrderService
{
	@Autowired
	private OrdersRepository ordersRepository;
	
	@Autowired
	private EmployeeOrdersRepository employeeOrdersRepository;
	
	public void storeUserOrders(Orders orders)
	{
		ordersRepository.save(orders);
	}
	
	/**
	 * Handles the complete employee sell-course flow:
	 * 1. Sets purchase date/time on the order
	 * 2. Stores the order
	 * 3. Creates the employee-order link
	 */
	public void storeEmployeeSale(Orders orders, String employeeEmail)
	{
		orders.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
		ordersRepository.save(orders);
		
		EmployeeOrders employeeOrders = new EmployeeOrders();
		employeeOrders.setOrderId(orders.getOrderId());
		employeeOrders.setEmployeeEmail(employeeEmail);
		employeeOrdersRepository.save(employeeOrders);
	}
}