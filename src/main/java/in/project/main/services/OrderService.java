package in.project.main.services;

import java.util.Optional;
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

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CouponService couponService;

	@Autowired(required = false)
	private AuditLogService auditLogService;

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

		Payment payment = paymentRepository.findByOrderId(order.getOrderId());
		if (payment == null)
		{
			payment = new Payment();
			payment.setOrderId(order.getOrderId());
		}
		payment.setPaymentId(paymentId);
		payment.setUserEmail(order.getUserEmail());
		payment.setAmount(order.getCourseAmount());
		payment.setStatus("SUCCESS");
		payment.setPaymentMethod("RAZORPAY");
		payment.setPaymentDate(DateTimeUtil.getCurrentDateTimeFormatted());
		paymentRepository.save(payment);

		// Increment coupon usage if applied
		if (order.getCouponCode() != null && !order.getCouponCode().trim().isEmpty())
		{
			couponService.incrementCouponUsage(order.getCouponCode().trim());
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

		Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());
		if (existingOpt.isPresent())
		{
			Enrollment existing = existingOpt.get();
			existing.setStatus(EnrollmentStatus.ACTIVE);
			existing.setPaymentStatus("PAID");
			existing.setEnrollmentType("PAID");
			existing.setEnrollmentSource("STUDENT_PURCHASE");
			existing.setOrderId(order.getOrderId());
			existing.setEnrolledAt(java.time.LocalDateTime.now());
			existing.setStartDate(java.time.LocalDateTime.now());
			existing.setStatusReason(null);
			enrollmentRepository.save(existing);
		}
		else
		{
			Enrollment enrollment = new Enrollment();
			enrollment.setUser(user);
			enrollment.setCourse(course);
			enrollment.setStatus(EnrollmentStatus.ACTIVE);
			enrollment.setPaymentStatus("PAID");
			enrollment.setEnrollmentType("PAID");
			enrollment.setEnrollmentSource("STUDENT_PURCHASE");
			enrollment.setOrderId(order.getOrderId());
			enrollment.setEnrolledAt(java.time.LocalDateTime.now());
			enrollment.setStartDate(java.time.LocalDateTime.now());
			enrollmentRepository.save(enrollment);
		}

		// Dispatch Notifications
		try {
			// 1. Student Payment & Enrollment Notifications
			notificationService.sendToUser(
				user.getEmail(),
				in.project.main.entities.enums.NotificationType.PAYMENT_SUCCESS,
				"Payment Successful",
				"Your payment of ₹" + order.getCourseAmount() + " for '" + course.getName() + "' was successfully processed (Order: " + order.getOrderId() + ").",
				"/student/orders",
				"ORDER",
				order.getOrderId()
			);

			notificationService.sendToUser(
				user.getEmail(),
				in.project.main.entities.enums.NotificationType.COURSE_ENROLLED,
				"Course Access Granted",
				"You are now enrolled in '" + course.getName() + "'. Start learning today!",
				"/student/courses/" + course.getId() + "/player",
				"COURSE",
				String.valueOf(course.getId())
			);

			// 2. Admin Notification
			notificationService.sendToAdmin(
				in.project.main.entities.enums.NotificationType.PAYMENT_RECEIVED,
				"Payment Received",
				"Payment of ₹" + order.getCourseAmount() + " received from " + user.getName() + " (" + user.getEmail() + ") for '" + course.getName() + "'.",
				"/admin/orders",
				"ORDER",
				order.getOrderId(),
				user.getEmail(),
				user.getName()
			);

			// 3. Instructor Notification
			String instructorEmail = null;
			if (course.getInstructorRef() != null && course.getInstructorRef().getEmail() != null) {
				instructorEmail = course.getInstructorRef().getEmail();
			} else if (course.getInstructorEmail() != null && !course.getInstructorEmail().isBlank()) {
				instructorEmail = course.getInstructorEmail();
			}

			if (instructorEmail != null && !instructorEmail.isBlank()) {
				notificationService.sendToInstructor(
					instructorEmail,
					in.project.main.entities.enums.NotificationType.COURSE_ENROLLED,
					"New Student Enrollment",
					user.getName() + " enrolled in your course '" + course.getName() + "'.",
					"/instructor/students",
					"COURSE",
					String.valueOf(course.getId())
				);
			}

			// 4. Audit Log Recording
			if (auditLogService != null) {
				in.project.main.events.PlatformAuditEvent payAudit = in.project.main.events.PlatformAuditEvent.of(
					user.getEmail(),
					in.project.main.entities.enums.AuditEventType.PAYMENT_SUCCESS,
					"PAYMENT_SETTLED",
					"Payment of ₹" + order.getCourseAmount() + " completed for course '" + course.getName() + "' (Order ID: " + order.getOrderId() + ", Payment ID: " + (paymentId != null ? paymentId : "N/A") + ")."
				)
				.withActor(String.valueOf(user.getId()), user.getEmail(), user.getName(), "STUDENT")
				.withEntity("PAYMENT", order.getOrderId(), course.getName())
				.withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
				.withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

				auditLogService.record(payAudit);

				in.project.main.events.PlatformAuditEvent enrollAudit = in.project.main.events.PlatformAuditEvent.of(
					user.getEmail(),
					in.project.main.entities.enums.AuditEventType.ENROLLMENT_CREATED,
					"COURSE_ENROLLED",
					"Student '" + user.getName() + "' enrolled in course '" + course.getName() + "' following successful payment."
				)
				.withActor(String.valueOf(user.getId()), user.getEmail(), user.getName(), "STUDENT")
				.withEntity("COURSE", String.valueOf(course.getId()), course.getName())
				.withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
				.withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

				auditLogService.record(enrollAudit);
			}
		} catch (Exception notifEx) {
			log.error("Failed to trigger post-payment notifications or audit: {}", notifEx.getMessage());
		}

		return true;
	}
}