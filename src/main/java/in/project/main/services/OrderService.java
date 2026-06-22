package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.entities.Orders;
import in.project.main.repositories.OrdersRepository;

@Service
public class OrderService
{
	@Autowired
	private OrdersRepository ordersRepository;
	
	public void storeUserOrders(Orders orders)
	{
		ordersRepository.save(orders);
	}
}