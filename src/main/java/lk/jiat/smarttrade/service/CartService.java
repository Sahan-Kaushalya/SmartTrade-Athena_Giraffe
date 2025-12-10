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
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CartService {
    private static final int MINIMUM_PRODUCT_QTY = 0;

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
        } else if (qty == null || !qty.matches(Validator.IS_INTEGER)) {
            message = "Invalid product quantity!";
        } else if (Integer.parseInt(qty) <= CartService.MINIMUM_PRODUCT_QTY) {
            message = "Product quantity must be greater than zero";
        } else {
            int productId = Integer.parseInt(prId);
            int reqQty = Integer.parseInt(qty);

            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            Stock stock = hibernateSession.createQuery("FROM Stock s WHERE s.product.id=:id", Stock.class)
                    .setParameter("id", productId)
                    .getSingleResultOrNull();
            if (stock == null) {
                message = "Product not found in stock!";
            } else {
                if (stock.getQty() < reqQty) {
                    message = "Insufficient product quantity!";
                } else {
                    // check is user logged in
                    HttpSession httpSession = request.getSession();
                    if (httpSession.getAttribute("user") == null) {
                        // user not logged in
                        status = true;
                        if (httpSession.getAttribute("sessionCart") == null) {
                            // first time add product
                            noSessionFirstTime(stock, reqQty, httpSession);
                            message = "User cart updated successfully";
                        } else {
                            // second time add product
                            responseObject = noSessionSecondTime(stock, reqQty, httpSession);
                        }
                    } else {
                        // user already logged
                        status = true;
                        if (httpSession.getAttribute("sessionCart") == null) {
                            // logged without session cart
                            responseObject = loggedWithOutSessionCart(stock, reqQty, httpSession);
                        } else {
                            // logged with session cart
                            responseObject = loggedWithSessionCart(stock, reqQty, httpSession);

                        }
                    }


                }
            }

            hibernateSession.close();
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    private JsonObject loggedWithOutSessionCart(Stock stock, int reqQty, HttpSession httpSession) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        User sessionUser = (User) httpSession.getAttribute("user");
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Cart existingCart = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user AND c.stock=:stock", Cart.class)
                .setParameter("user", sessionUser)
                .setParameter("stock", stock)
                .getSingleResultOrNull();
        try (hibernateSession) { // try with resources. HibernateSession is a closable object.
            Transaction transaction = hibernateSession.beginTransaction();
            if (existingCart == null) {
                // db cart not found (one item). add as a new cart item
                Cart cart = new Cart();
                cart.setUser(sessionUser);
                cart.setStock(stock);
                cart.setQty(reqQty);
                hibernateSession.persist(cart);
                status = true;
                message = "Product add to the cart";
            } else {
                // db cart already exists. update quantity
                int newQty = existingCart.getQty() + reqQty;
                if (newQty > stock.getQty()) {
                    message = "Product quantity exceeded!";
                } else {
                    existingCart.setQty(newQty);
                    hibernateSession.merge(existingCart);
                    status = true;
                    message = "User cart updated";
                }
            }
            transaction.commit();

        } catch (HibernateException e) {
            throw new RuntimeException(e);
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return responseObject;
    }

    private JsonObject loggedWithSessionCart(Stock stock, int reqQty, HttpSession httpSession) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        List<Cart> sessionCart = getSessionAttribute(httpSession);
        User sessionUser = (User) httpSession.getAttribute("user");
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = hibernateSession.beginTransaction();
        try (hibernateSession) {
            boolean isFound = false;
            Cart cart = null;
            for (Cart c : sessionCart) {
                Cart existingCart = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user AND c.stock=:stock", Cart.class)
                        .setParameter("user", sessionUser)
                        .setParameter("stock", c.getStock())
                        .getSingleResultOrNull();
                if (existingCart != null) {
                    isFound = true;
                    cart = existingCart;
                }
            }
            if (isFound) {
                int newQty = cart.getQty() + reqQty;
                if (newQty > stock.getQty()) {
                    message = "Product quantity exceeded!";
                } else {
                    cart.setQty(newQty);
                    hibernateSession.merge(cart);
                    transaction.commit();
                    status = true;
                    message = "User cart updated!";
                }

            } else {
                cart = new Cart();
                cart.setUser(sessionUser);
                cart.setQty(reqQty);
                cart.setStock(stock);
                hibernateSession.persist(cart);
                transaction.commit();
                status = true;
                message = "Product add to the cart";
            }

        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return responseObject;
    }

    private JsonObject noSessionSecondTime(Stock stock, int reqQty, HttpSession httpSession) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        List<Cart> sessionCart = getSessionAttribute(httpSession); // helper method calling. otherwise we need to cast Object to List
        boolean isFound = false; // check already same stock exists
        Cart cart = null;
        for (Cart c : sessionCart) {
            if (c.getStock().getId() == stock.getId()) {
                isFound = true;
                cart = c;
                break;
            }
        }
        if (isFound) {
            int newQty = cart.getQty() + reqQty;
            if (newQty > stock.getQty()) {
                message = "Product quantity exceeded!";
            } else {
                cart.setQty(newQty);
                status = true;
                message = "User cart updated!";
            }
        } else {
            cart = new Cart();
            cart.setId(sessionCart.size() + 1);
            cart.setUser(null);
            cart.setStock(stock);
            cart.setQty(reqQty);
            sessionCart.add(cart);
            status = true;
            message = "Product add to the cart";
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return responseObject;
    }

    // helper method for the avoid warning
    @SuppressWarnings("unchecked")
    private <T> T getSessionAttribute(HttpSession httpSession) {
        return (T) httpSession.getAttribute("sessionCart");
    }

    private void noSessionFirstTime(Stock stock, int reqQty, HttpSession httpSession) {
        List<Cart> cartList = new ArrayList<>();
        Cart cart = new Cart();
        cart.setId(1);
        cart.setUser(null);
        cart.setStock(stock);
        cart.setQty(reqQty);
        cartList.add(cart);
        httpSession.setAttribute("sessionCart", cartList);
    }

    public String getLoadCartItems(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        // check is user logged in or not
        HttpSession httpSession = request.getSession();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        if (httpSession.getAttribute("user") == null) {
            // session cart
            List<Cart> sessionCart = getSessionAttribute(httpSession);
            if (sessionCart == null) {
                message = "Your cart is empty!";
            } else {
                if (sessionCart.isEmpty()) {
                    message = "Your cart is empty!";
                } else {
                    responseObject.add("cartList", AppUtil.GSON.toJsonTree(getCartDTOList(sessionCart)));
                    status = true;
                    message = "Cart items loaded successfully";
                }
            }
        } else {
            // db cart
            User sessionUser = (User) httpSession.getAttribute("user");
            User dbUser = hibernateSession.createQuery("FROM User u WHERE u=:user", User.class)
                    .setParameter("user", sessionUser)
                    .getSingleResult();
            Set<Cart> carts = dbUser.getCarts();
            if (carts.isEmpty()) {
                message = "Your cart is empty!";
            } else {
                responseObject.add("cartList", AppUtil.GSON.toJsonTree(getCartDTOList(carts.stream().toList())));
                status = true;
                message = "Cart items loaded successfully";
            }

        }
        hibernateSession.close();
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    private List<CartDTO> getCartDTOList(List<Cart> cartList) {
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        List<CartDTO> cartDTOList = new ArrayList<>();
        for (Cart cart : cartList) {
            Stock stock = hibernateSession.createQuery("FROM Stock s WHERE s=:stock", Stock.class)
                    .setParameter("stock", cart.getStock())
                    .getSingleResult();
            CartDTO cartDTO = new CartDTO();
            cartDTO.setCartId(cart.getId());
            cartDTO.setProductId(stock.getProduct().getId());
            cartDTO.setStockId(stock.getId());
            cartDTO.setImages(stock.getProduct().getImages());
            cartDTO.setTitle(stock.getProduct().getTitle());
            cartDTO.setPrice(stock.getPrice());
            cartDTO.setQty(cart.getQty());
            cartDTOList.add(cartDTO);
        }
        hibernateSession.close();
        return cartDTOList;
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
