package lk.jiat.smarttrade.service;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import lk.jiat.smarttrade.dto.*;
import lk.jiat.smarttrade.entity.*;
import lk.jiat.smarttrade.util.AppUtil;
import lk.jiat.smarttrade.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;
import java.util.stream.Collectors;

public class CheckoutService {

    public String loadCheckoutData(HttpServletRequest request) {
        System.out.println("load checkout work");
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            responseObject.addProperty("message", "Unauthorized");
            return AppUtil.GSON.toJson(responseObject);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        try {
            CheckoutDTO dto = new CheckoutDTO();

            // load latest address
            Address address = hibernateSession.createQuery(
                            "SELECT a FROM Address a WHERE a.user = :user ORDER BY a.id DESC", Address.class)
                    .setParameter("user", sessionUser)
                    .setMaxResults(1)
                    .uniqueResult();

            if (address != null) {
                UserAddressDTO addrDto = new UserAddressDTO();
                addrDto.setId(address.getId());
                addrDto.setLineOne(address.getLineOne());
                addrDto.setLineTwo(address.getLineTwo());
                addrDto.setPostalCode(address.getPostalCode());
                addrDto.setMobile(address.getMobile());
                addrDto.setPrimary(address.isPrimary());

                // city
                CityDTO cityDto = new CityDTO();
                cityDto.setId(address.getCity().getId());
                cityDto.setName(address.getCity().getName());
                addrDto.setCity(cityDto);

                // user
                UserDTO userDto = new UserDTO();
                userDto.setFirstName(address.getUser().getFirstName());
                userDto.setLastName(address.getUser().getLastName());
                addrDto.setUser(userDto);

                dto.setUserAddress(addrDto);
                dto.setStatus(true);
            } else {
                message = "Your account details are incomplete. Please fill your shipping address.";
            }

            // load cities
            List<City> cityList = hibernateSession.createQuery("SELECT c FROM City c ORDER BY c.name ASC", City.class).list();
            List<CityDTO> cityDtos = cityList.stream()
                    .map(c -> {
                        CityDTO cd = new CityDTO();
                        cd.setId(c.getId());
                        cd.setName(c.getName());
                        return cd;
                    })
                    .collect(Collectors.toList());
            dto.setCityList(cityDtos);

            // load cart
            List<Cart> cartList = hibernateSession.createQuery(
                            "SELECT c FROM Cart c WHERE c.user = :user", Cart.class)
                    .setParameter("user", sessionUser)
                    .list();

            if (cartList.isEmpty()) {
                dto.setMessage("empty-cart");
            } else {
                List<CartDTO> cartDtos = cartList.stream()
                        .map(cart -> {
                            CartDTO cdto = new CartDTO();
                            cdto.setCartId(cart.getId());
                            cdto.setQty(cart.getQty());

                            Stock stock = cart.getStock();
                            cdto.setStockId(stock.getId());
                            cdto.setPrice(stock.getPrice());
                            cdto.setAvailableQty(stock.getQty());
                            cdto.setDiscount(stock.getDiscount() != null ? stock.getDiscount().getValue() : 0.0);
                            cdto.setStatus(stock.getStatus() != null ? stock.getStatus().getValue() : "Unknown");

                            Product product = stock.getProduct();
                            cdto.setProductId(product.getId());
                            cdto.setTitle(product.getTitle());
                            cdto.setDescription(product.getDescription());
                            cdto.setImages(product.getImages());

                            return cdto;
                        })
                        .collect(Collectors.toList());

                dto.setCartList(cartDtos);


                // delivery type
                List<DeliveryType> deliveryTypes = hibernateSession.createQuery("SELECT d FROM DeliveryType d", DeliveryType.class).list();
                List<DeliveryTypeDTO> deliveryDtos = deliveryTypes.stream()
                        .map(dt -> {
                            DeliveryTypeDTO dtd = new DeliveryTypeDTO();
                            dtd.setId(dt.getId());
                            dtd.setName(dt.getName());
                            dtd.setPrice(dt.getPrice());
                            return dtd;
                        })
                        .collect(Collectors.toList());

                dto.setDeliveryTypes(deliveryDtos);
                dto.setStatus(true);
            }

            if (dto.getUserAddress() != null || !dto.getCityList().isEmpty()) {
                dto.setStatus(true);
            }

            return AppUtil.GSON.toJson(dto);

        } catch (Exception e) {
            e.printStackTrace();
            status = false;
            message = "Something went wrong while loading checkout data.";
        } finally {
            hibernateSession.close();

        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String processCheckout(CheckoutRequestDTO dto, HttpServletRequest request) {
        return null;
    }


}
