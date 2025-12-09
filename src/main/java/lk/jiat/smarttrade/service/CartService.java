package lk.jiat.smarttrade.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lk.jiat.smarttrade.dto.CartDTO;
import lk.jiat.smarttrade.entity.Cart;
import lk.jiat.smarttrade.entity.Product;
import lk.jiat.smarttrade.entity.Stock;
import lk.jiat.smarttrade.entity.User;
import lk.jiat.smarttrade.util.AppUtil;
import lk.jiat.smarttrade.util.HibernateUtil;
import lk.jiat.smarttrade.validation.Validator;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CartService {

    public void checkSessionCart(User user, HttpServletRequest request) {
        HttpSession session = request.getSession();
        @SuppressWarnings("unchecked")
        List<Map<String, Integer>> sessionCart =
                (List<Map<String, Integer>>) session.getAttribute("sessionCart");

        if (sessionCart == null || sessionCart.isEmpty()) {
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            User managedUser = s.get(User.class, user.getId());

            List<Integer> stockIds = sessionCart.stream()
                    .map(item -> item.get("stockId"))
                    .collect(Collectors.toList());  //convert stream into list

            List<Cart> existingCarts = s.createQuery(
                            "SELECT c FROM Cart c WHERE c.user = :user AND c.stock.id IN :ids", Cart.class)
                    .setParameter("user", managedUser)
                    .setParameter("ids", stockIds)
                    .getResultList();

            Map<Integer, Cart> existingCartMap = existingCarts.stream()
                    .collect(Collectors.toMap(c -> c.getStock().getId(), Function.identity()));

            for (Map<String, Integer> item : sessionCart) {
                Integer stockId = item.get("stockId");
                Integer guestQty = item.get("qty");

                if (guestQty == null || guestQty <= 0) continue;

                Stock stock = s.get(Stock.class, stockId);
                if (stock == null || guestQty > stock.getQty()) {
                    continue;
                }

                Cart existing = existingCartMap.get(stockId);
                if (existing != null) {
                    int newQty = existing.getQty() + guestQty;
                    if (newQty <= stock.getQty()) {
                        existing.setQty(newQty);
                    } else {
                        existing.setQty(stock.getQty());
                    }
                    s.merge(existing);
                } else {
                    // add new cart item
                    Cart newCart = new Cart();
                    newCart.setUser(managedUser);
                    newCart.setStock(stock);
                    newCart.setQty(guestQty);
                    s.persist(newCart);
                }
            }

            tx.commit();

            session.removeAttribute("sessionCart");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addToCart(String prId, String qty, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        if (prId == null || !prId.matches(Validator.IS_INTEGER)) {
            message = "Invalid product id!";
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }
        if (qty == null || !qty.matches(Validator.IS_INTEGER)) {
            message = "Invalid quantity!";
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }

        int productId = Integer.parseInt(prId);
        int requestedQty = Integer.parseInt(qty);

        if (requestedQty <= 0) {
            message = "Quantity must be greater than 0.";
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = hibernateSession.beginTransaction();

            Stock stock = hibernateSession.createQuery(
                            "SELECT s FROM Stock s WHERE s.product.id = :productId", Stock.class)
                    .setParameter("productId", productId)
                    .uniqueResult();

            if (stock == null) {
                message = "Product not available in stock.";
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", message);
                return AppUtil.GSON.toJson(responseObject);
            }

            if (requestedQty > stock.getQty()) {
                message = "Insufficient stock! Available: " + stock.getQty();
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", message);
                return AppUtil.GSON.toJson(responseObject);
            }

            User sessionUser = (User) request.getSession().getAttribute("user");

            if (sessionUser != null) {
                User user = hibernateSession.get(User.class, sessionUser.getId());

                Cart existingCart = hibernateSession.createQuery(
                                "SELECT c FROM Cart c WHERE c.user = :user AND c.stock = :stock", Cart.class)
                        .setParameter("user", user)
                        .setParameter("stock", stock)
                        .uniqueResult();

                if (existingCart == null) {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setStock(stock);
                    cart.setQty(requestedQty);
                    hibernateSession.persist(cart);
                    message = "Product added to cart successfully";
                } else {
                    int newQty = existingCart.getQty() + requestedQty;
                    if (newQty <= stock.getQty()) {
                        existingCart.setQty(newQty);
                        hibernateSession.merge(existingCart);
                        message = "Cart updated successfully";
                    } else {
                        message = "Cannot add more. Max available: " + stock.getQty();
                        responseObject.addProperty("status", false);
                        responseObject.addProperty("message", message);
                        return AppUtil.GSON.toJson(responseObject);
                    }
                }
                status = true;

            } else {
                // GUEST USER
                HttpSession httpSession = request.getSession();
                @SuppressWarnings("unchecked")
                List<Map<String, Integer>> sessionCart =
                        (List<Map<String, Integer>>) httpSession.getAttribute("sessionCart");

                if (sessionCart == null) {
                    sessionCart = new ArrayList<>();
                    httpSession.setAttribute("sessionCart", sessionCart);
                }

                boolean found = false;
                for (Map<String, Integer> item : sessionCart) {
                    if (item.get("stockId").equals(stock.getId())) {
                        int newQty = item.get("qty") + requestedQty;
                        if (newQty <= stock.getQty()) {
                            item.put("qty", newQty);
                            message = "Session cart updated";
                        } else {
                            message = "Cannot add more. Max available: " + stock.getQty();
                            responseObject.addProperty("status", false);
                            responseObject.addProperty("message", message);
                            return AppUtil.GSON.toJson(responseObject);
                        }
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Map<String, Integer> newItem = new HashMap<>();
                    newItem.put("stockId", stock.getId());
                    newItem.put("qty", requestedQty);
                    sessionCart.add(newItem);
                    message = "Product added to session cart";
                }
                status = true;
            }

            tx.commit();
            responseObject.addProperty("status", status);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);

        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Something went wrong. Please try again.");
            return AppUtil.GSON.toJson(responseObject);
        }
    }


    public String getLoadCartItems(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Cart> cartList = new ArrayList<>();

            if (sessionUser != null) {
                // LOGGED USER -> load db cart
                cartList = s.createQuery(
                                "SELECT c FROM Cart c JOIN FETCH c.stock s JOIN FETCH s.product p WHERE c.user.id = :uid", Cart.class)
                        .setParameter("uid", sessionUser.getId())
                        .list();
            } else {
                // GUEST USER -> load session cart
                @SuppressWarnings("unchecked")
                List<Map<String, Integer>> sessionCartRaw =
                        (List<Map<String, Integer>>) request.getSession().getAttribute("sessionCart");

                if (sessionCartRaw != null && !sessionCartRaw.isEmpty()) {
                    List<Integer> stockIds = sessionCartRaw.stream()
                            .map(item -> item.get("stockId"))
                            .collect(Collectors.toList());

                    List<Stock> stocks = s.createQuery(
                                    "SELECT s FROM Stock s JOIN FETCH s.product p WHERE s.id IN :ids", Stock.class)
                            .setParameter("ids", stockIds)
                            .getResultList();

                    Map<Integer, Stock> stockMap = stocks.stream()
                            .collect(Collectors.toMap(Stock::getId, Function.identity()));

                    for (Map<String, Integer> item : sessionCartRaw) {
                        Integer stockId = item.get("stockId");
                        Integer qty = item.get("qty");

                        Stock stock = stockMap.get(stockId);
                        if (stock != null) {
                            Cart cart = new Cart();
                            cart.setStock(stock);
                            cart.setQty(qty);
                            cartList.add(cart);
                        }
                    }
                }
            }

            if (cartList.isEmpty()) {
                status = false;
                message = "Your cart is empty.";
                responseObject.addProperty("status", status);
                responseObject.addProperty("message", message);
                return AppUtil.GSON.toJson(responseObject);
            }

            JsonArray cartArray = new JsonArray();
            for (Cart c : cartList) {
                Stock stock = c.getStock();
                Product product = stock.getProduct();

                CartDTO dto = new CartDTO();
                dto.setCartId(c.getId() != 0 ? c.getId() : 0);
                dto.setQty(c.getQty());
                dto.setStockId(stock.getId());
                dto.setPrice(stock.getPrice());
                dto.setAvailableQty(stock.getQty());
                dto.setDiscount(stock.getDiscount() != null ? stock.getDiscount().getValue() : 0.0);
                dto.setStatus(stock.getStatus() != null ? stock.getStatus().getValue() : "Unknown");
                dto.setProductId(product.getId());
                dto.setTitle(product.getTitle());
                dto.setDescription(product.getDescription());
                dto.setImages(product.getImages());

                cartArray.add(AppUtil.GSON.toJsonTree(dto));
            }

            status = true;
            message = "Cart loaded successfully.";
            responseObject.addProperty("status", status);
            responseObject.addProperty("message", message);
            responseObject.add("cartItems", cartArray);

        } catch (Exception e) {
            e.printStackTrace();
            status = false;
            message = "Something went wrong while loading cart.";
            responseObject.addProperty("status", status);
            responseObject.addProperty("message", message);
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String removeCartItem(int cartId, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        response.addProperty("status", false);

        User sessionUser = (User) request.getSession().getAttribute("user");

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            Cart cart = s.get(Cart.class, cartId);

            if (cart == null) {
                response.addProperty("message", "Cart item not found");
            } else if (sessionUser != null && cart.getUser().getId() != sessionUser.getId()) {
                response.addProperty("message", "Unauthorized");
            } else {
                s.remove(cart);
                tx.commit();
                response.addProperty("status", true);
                response.addProperty("message", "Item removed successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Something went wrong");
        }

        return AppUtil.GSON.toJson(response);
    }

}
