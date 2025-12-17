package lk.jiat.smarttrade.service;

import lk.jiat.smarttrade.entity.*;
import lk.jiat.smarttrade.util.HibernateUtil;
import lk.jiat.smarttrade.validation.Validator;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Set;

public class OrderService {
    public void completeOrder(String orderId) {
        int oid = Integer.parseInt(orderId.replaceAll(Validator.NON_DIGIT_PATTERN, ""));

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Order order = hibernateSession.find(Order.class, oid);
                if (order == null) {
                    throw new RuntimeException("Order not found for ID: " + oid);
                }

                // Update stock quantities
                Set<OrderItem> orderItems = order.getOrderItems();
                if (orderItems != null) {
                    for (OrderItem orderItem : orderItems) {
                        Stock stock = orderItem.getStock();
                        if (stock != null) {
                            int updatedQty = stock.getQty() - orderItem.getQty();
                            if (updatedQty < 0) {
                                throw new RuntimeException("Insufficient stock for product: "
                                        + stock.getProduct().getTitle());
                            }
                            stock.setQty(updatedQty);
                            hibernateSession.merge(stock);
                        }
                    }
                }

                // Update order status
                Status completedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.COMPLETED))
                        .getSingleResult();
                order.setStatus(completedStatus);
                hibernateSession.merge(order);

                // Remove cart items
                List<Cart> cartList = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user", Cart.class)
                        .setParameter("user", order.getUser())
                        .getResultList();
                for (Cart cart : cartList) {
                    hibernateSession.remove(cart);
                }

                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException("Failed to complete order: " + e.getMessage(), e);
            }
        }
    }

    public void failOrder(String orderId) {
        int oid = Integer.parseInt(orderId.replaceAll(Validator.NON_DIGIT_PATTERN, ""));

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Order order = hibernateSession.find(Order.class, oid);
                if (order == null) {
                    throw new RuntimeException("Order not found for ID: " + oid);
                }

                Status rejectedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.REJECTED))
                        .getSingleResult();
                order.setStatus(rejectedStatus);
                hibernateSession.merge(order);

                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException("Failed to reject order: " + e.getMessage(), e);
            }
        }
    }
}
