package lk.jiat.smarttrade.service;

import com.google.gson.JsonObject;
import lk.jiat.smarttrade.dto.ProductDTO;
import lk.jiat.smarttrade.entity.*;
import lk.jiat.smarttrade.util.AppUtil;
import lk.jiat.smarttrade.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

public class AdvancedSearchService {
    public String getAllProductData() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        // load all brands
        List<Brand> brands = hibernateSession.createQuery("FROM Brand b", Brand.class).getResultList();
        List<JsonObject> brandList = ContentService.brands(brands);

        // load all conditions
        List<Quality> qualityList = hibernateSession.createQuery("FROM Quality q", Quality.class).getResultList();

        // load all colors
        List<Color> colorList = hibernateSession.createQuery("FROM Color c", Color.class).getResultList();

        // load all storages
        List<Storage> storageList = hibernateSession.createQuery("FROM Storage s", Storage.class).getResultList();

        Double minPrice = hibernateSession.createQuery("SELECT MIN(s.price) FROM Stock s", Double.class).uniqueResult();
        Double maxPrice = hibernateSession.createQuery("SELECT MAX(s.price) FROM Stock s", Double.class).uniqueResult();

        Status approvedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                .setParameter("value", String.valueOf(Status.Type.APPROVED))
                .getSingleResult();
        Query<Stock> stockQuery = hibernateSession.createQuery("FROM Stock s WHERE s.status=:status ORDER BY s.id ASC", Stock.class)
                .setParameter("status", approvedStatus);

        // set all product count
        responseObject.addProperty("allProductCount", stockQuery.getResultList().size()); // as an example - product count ==> 100

        // get stock list
        stockQuery.setFirstResult(AppUtil.FIRST_RESULT_VALUE);
        stockQuery.setMaxResults(AppUtil.MAX_RESULT_VALUE); // Getting stocks from 0 to 10
        List<Stock> stockList = stockQuery.getResultList(); // return 10 stocks

        List<ProductDTO> productDTOList = new ArrayList<>();
        for (Stock stock : stockList) {
            ProductDTO productDTO = new ProductDTO();
            productDTO.setStockId(stock.getId());
            productDTO.setTitle(stock.getProduct().getTitle());
            productDTO.setPrice(stock.getPrice());
            productDTO.setImages(stock.getProduct().getImages());
            productDTOList.add(productDTO);
        }

        hibernateSession.close();

        // attach value to response object
        responseObject.add("brandList", AppUtil.GSON.toJsonTree(brandList));
        responseObject.add("qualityList", AppUtil.GSON.toJsonTree(qualityList));
        responseObject.add("colorList", AppUtil.GSON.toJsonTree(colorList));
        responseObject.add("storageList", AppUtil.GSON.toJsonTree(storageList));
        responseObject.add("productList", AppUtil.GSON.toJsonTree(productDTOList));
        responseObject.addProperty("minPrice", minPrice);
        responseObject.addProperty("maxPrice", maxPrice);
        responseObject.addProperty("maxResult", AppUtil.MAX_RESULT_VALUE);

        status = true;

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
