package in.project.main.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Course;
import in.project.main.entities.EmployeeOrders;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Orders;
import in.project.main.entities.Payment;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.EmployeeOrdersRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.util.DateTimeUtil;

@Service
public class OrderService
{
	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	@Autowired
	private OrdersRepository ordersRepository;

	@Autowired
	private EmployeeOrdersRepository employeeOrdersRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private UserRepository userRepository;

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

	/**
	 * Settles a payment that has already passed signature verification.
	 *
	 * The order must be the PENDING row this application created in createOrder, and the
	 * course must be the one recorded on that row -- the caller is responsible for both
	 * checks, because trusting a client-supplied course here would let a buyer pay for a
	 * cheap course and be enrolled in an expensive one.
	 *
	 * Order status, payment record and enrollment are written in a single transaction so a
	 * failure part-way through cannot leave a COMPLETED order with no enrollment.
	 *
	 * @return true if this call settled the order, false if it was already settled
	 */
	@Transactional
	public boolean settleVerifiedPayment(Orders order, Course course, String paymentId, String signature)
	{
		if ("COMPLETED".equals(order.getStatus()))
		{
			return false;
		}

		order.setPaymentId(paymentId);
		order.setSignature(signature);
		order.setStatus("COMPLETED");
		ordersRepository.save(order);

		if (paymentRepository.findByOrderId(order.getOrderId()) == null)
		{
			Payment payment = new Payment();
			payment.setOrderId(order.getOrderId());
			payment.setAmount(order.getCourseAmount());
			payment.setStatus("SUCCESS");
			payment.setPaymentMethod("RAZORPAY");
			payment.setPaymentDate(DateTimeUtil.getCurrentDateTimeFormatted());
			paymentRepository.save(payment);
		}

		User user = userRepository.findByEmail(order.getUserEmail());
		if (user == null)
		{
			// The money is real and already captured, so the order and payment rows must
			// stand. Only a student account can hold an enrollment, so flag this instead of
			// rolling back and losing the payment record.
			log.error("Settled order {} but no student account exists for its buyer - enrollment not created",
					order.getOrderId());
			return true;
		}

		if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId()))
		{
			Enrollment enrollment = new Enrollment();
			enrollment.setUser(user);
			enrollment.setCourse(course);
			enrollment.setStatus(EnrollmentStatus.ACTIVE);
			enrollment.setPaymentStatus("PAID");
			enrollmentRepository.save(enrollment);
		}

		return true;
	}
}