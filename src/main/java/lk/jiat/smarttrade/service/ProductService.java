package lk.jiat.smarttrade.service;

import com.google.gson.JsonObject;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;
import lk.jiat.smarttrade.dto.ProductDTO;
import lk.jiat.smarttrade.dto.StockDTO;
import lk.jiat.smarttrade.entity.*;
import lk.jiat.smarttrade.util.AppUtil;
import lk.jiat.smarttrade.util.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class ProductService {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MMMM dd");
    private static final int MAX_RESULT = 6;

    // make method for the advanced search mechanism
    public String loadAdvancedSearchData(JsonObject requestObject) {
        JsonObject responseObject = new JsonObject();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

        StringBuilder hql = new StringBuilder(
                "SELECT s FROM Stock s " +
                        "JOIN s.product p " +
                        "LEFT JOIN p.model m " +
                        "LEFT JOIN m.brand b " +
                        "LEFT JOIN p.color c " +
                        "LEFT JOIN p.quality q " +
                        "LEFT JOIN p.storage st " +
                        "WHERE 1=1 "
        ); // methana WHERE 1=1 kiyala daganne passe query ekata ekathu karanna lesi wenna. always true condition
        // ekak thamai set karala thiyenne
        // methanin pahala tika JsonObject eke thiyena dewal aragena where ekata ekathu karanna hadanne
        Map<String, Object> params = new HashMap<>();

        if (requestObject.has("brandName")) {
            hql.append(" AND b.name = :brandName ");
            params.put("brandName", requestObject.get("brandName").getAsString());
        }

        if (requestObject.has("conditionName")) {
            hql.append(" AND q.value = :quality ");
            params.put("quality", requestObject.get("conditionName").getAsString());
        }

        if (requestObject.has("colorName")) {
            hql.append(" AND c.value = :color ");
            params.put("color", requestObject.get("colorName").getAsString());
        }

        if (requestObject.has("storageValue")) {
            hql.append(" AND st.value = :storage ");
            params.put("storage", requestObject.get("storageValue").getAsString());
        }

        if (requestObject.has("priceStart") && requestObject.has("priceEnd")) {
            hql.append(" AND s.price BETWEEN :startPrice AND :endPrice ");
            params.put("startPrice", requestObject.get("priceStart").getAsDouble());
            params.put("endPrice", requestObject.get("priceEnd").getAsDouble());
        }

        hql.append(" AND s.status.value = :approvedStatus ");
        params.put("approvedStatus", Status.Type.APPROVED.toString());
        if (requestObject.has("sortValue")) {
            String sortValue = requestObject.get("sortValue").getAsString();

            switch (sortValue) {
                case "Sort by Latest":
                    hql.append(" ORDER BY s.id DESC ");
                    break;
                case "Sort by Oldest":
                    hql.append(" ORDER BY s.id ASC ");
                    break;
                case "Sort by Name":
                    hql.append(" ORDER BY p.title ASC ");
                    break;
                case "Sort by Price":
                    hql.append(" ORDER BY s.price ASC ");
                    break;
            }
        }
        Query<Stock> query = hibernateSession.createQuery(hql.toString(), Stock.class);
        params.forEach(query::setParameter);
        if (requestObject.has("firstResult")) {
            int first = requestObject.get("firstResult").getAsInt();
            query.setFirstResult(first);
            query.setMaxResults(ProductService.MAX_RESULT);
        }
        List<Stock> stockList = query.getResultList();
        List<ProductDTO> productList = new ArrayList<>();
        for(Stock s:stockList){
            ProductDTO productDTO = new ProductDTO();
            productDTO.setProductId(s.getProduct().getId());
            productDTO.setTitle(s.getProduct().getTitle());
            productDTO.setImages(s.getProduct().getImages());
            productDTO.setPrice(s.getPrice());
            productList.add(productDTO);
        }
        responseObject.add("productList", AppUtil.GSON.toJsonTree(productList));
        String countHql = hql.toString().replace("SELECT s", "SELECT COUNT(s)");

        Query<Long> countQuery = hibernateSession.createQuery(countHql, Long.class);

        params.forEach(countQuery::setParameter);

        long count = countQuery.getSingleResult();
        responseObject.addProperty("allProductCount", count);


        hibernateSession.close();

        return AppUtil.GSON.toJson(responseObject);
    }

    // make method for the load similar products data
    public String getSimilarProducts(int productId) {
        JsonObject responseObject = new JsonObject();

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Product product = hibernateSession.find(Product.class, productId);

        // load similar product data
        List<Model> modelList = hibernateSession.createQuery("FROM Model m WHERE m.brand=:brand", Model.class)
                .setParameter("brand", product.getModel().getBrand())
                .getResultList();

        // make sub query using same model and except loaded product
        List<Product> productList = hibernateSession.createQuery("FROM Product p WHERE p.model IN :modelList AND p != :product", Product.class)
                .setParameter("modelList", modelList)
                .setParameter("product", product)
                .getResultList();

        List<ProductDTO> productDTOList = new ArrayList<>();
        for (Product p : productList) {
            ProductDTO productDTO = getProductDTO(p);
            productDTOList.add(productDTO);
        }

        responseObject.add("similarProducts", AppUtil.GSON.toJsonTree(productDTOList));
        hibernateSession.close();

        return AppUtil.GSON.toJson(responseObject);
    }

    private static ProductDTO getProductDTO(Product p) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductId(p.getId());
        productDTO.setTitle(p.getTitle());
        productDTO.setColorId(p.getColor().getId());
        productDTO.setColorValue(p.getColor().getValue());
        productDTO.setStorageId(p.getStorage().getId());
        productDTO.setStorageValue(p.getStorage().getValue());
        productDTO.setImages(p.getImages());
        List<StockDTO> stockDTOList = new ArrayList<>();
        for (Stock stock : p.getStocks()) {
            StockDTO stockDTO = new StockDTO();
            stockDTO.setPrice(stock.getPrice());
            stockDTOList.add(stockDTO);
        }

        productDTO.setStockDTOList(stockDTOList);
        return productDTO;
    }

    // make method fot the retrieve single product data
    public String getSingleProduct(int productId) {
        JsonObject responseObject = new JsonObject();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Product product = hibernateSession.find(Product.class, productId);
        ProductDTO productDTO = new ProductDTO();// make dto object for transfer product data
        productDTO.setProductId(productId);
        productDTO.setTitle(product.getTitle());
        productDTO.setDescription(product.getDescription());
        productDTO.setBrandName(product.getModel().getBrand().getName());
        productDTO.setModelName(product.getModel().getName());
        productDTO.setQualityValue(product.getQuality().getValue());

        productDTO.setColorValue(product.getColor().getValue());
        productDTO.setStorageValue(product.getStorage().getValue());

        List<StockDTO> stockDTOList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy");
        for (Stock stock : product.getStocks()) {
            StockDTO stockDTO = new StockDTO();
            stockDTO.setProductId(stock.getProduct().getId());
            stockDTO.setStockId(stock.getId());
            stockDTO.setQty(stock.getQty());
            stockDTO.setPrice(stock.getPrice());
            stockDTO.setCreatedAt(formatter.format(stock.getCreatedAt()));
            stockDTOList.add(stockDTO);
        }
        productDTO.setStockDTOList(stockDTOList);
        productDTO.setImages(product.getImages());
        responseObject.add("singleProduct", AppUtil.GSON.toJsonTree(productDTO));
        hibernateSession.close();
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getAllUserProducts(@Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        HttpSession httpSession = request.getSession(false);
        if (httpSession == null || httpSession.getAttribute("user") == null) {
            message = "Session Expired. Please logged in!";
        } else {
            User sessionUser = (User) httpSession.getAttribute("user");
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user=:user", Seller.class)
                    .setParameter("user", sessionUser)
                    .getSingleResultOrNull();
            if (seller == null) {
                message = "The requested profile is not a seller account. Please register first as a seller";
            } else {
                if (!seller.getStatus().getValue().equals(String.valueOf(Status.Type.APPROVED))) {
                    message = "Not approved account. Please be patient till admin approval!";
                } else {
                    Set<Product> productSet = seller.getProducts();
                    if (productSet.isEmpty()) {
                        message = "Products Not Found";
                    } else {
                        List<ProductDTO> productDTOList = new ArrayList<>();
                        for (Product p : productSet) {
                            ProductDTO dto = new ProductDTO();
                            dto.setProductId(p.getId());
                            dto.setTitle(p.getTitle());

                            List<Stock> stocks = hibernateSession.createQuery("FROM Stock s WHERE s.product=:product ORDER BY s.id DESC", Stock.class)
                                    .setParameter("product", p)
                                    .getResultList();

                            List<StockDTO> stockDTOList = new ArrayList<>();
                            for (Stock s : stocks) {
                                StockDTO stockDTO = new StockDTO();
                                stockDTO.setStockId(s.getId());
                                stockDTO.setProductId(s.getProduct().getId());
                                stockDTO.setQty(s.getQty());
                                stockDTO.setPrice(s.getPrice());
                                stockDTO.setCreatedAt(formatter.format(s.getCreatedAt()));
                                stockDTOList.add(stockDTO);
                            }
                            dto.setStockDTOList(stockDTOList);
                            productDTOList.add(dto);
                            stockDTOList.sort(Comparator.comparing(StockDTO::getStockId).reversed());
                            productDTOList.sort(Comparator.comparing(ProductDTO::getProductId).reversed());
                        }
                        responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOList));
                        status = true;
                        message = "Product loading successful";
                    }
                }
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);

        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateProduct(Product product) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = hibernateSession.beginTransaction();
        try {
            hibernateSession.merge(product);
            transaction.commit();
            status = true;
            message = "Product images uploading successful";
        } catch (HibernateException e) {
            transaction.rollback();
            message = "Product image uploading failed!";
        }
        hibernateSession.close();
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public Product getProductById(int id) {
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Product product = hibernateSession.find(Product.class, id);
        hibernateSession.close();
        return product;
    }

    public String addNewProduct(ProductDTO productDTO, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        /// product-inserting-part-start
        if (productDTO.getBrandId() <= 0) {
            message = "Invalid brand type. Please select a correct brand!";
        } else if (productDTO.getModelId() <= 0) {
            message = "Invalid model type. Please select a correct model!";
        } else if (productDTO.getTitle() == null) {
            message = "Product title is required!";
        } else if (productDTO.getTitle().isBlank()) {
            message = "Product title can not be empty!";
        } else if (productDTO.getDescription() == null) {
            message = "Product description is required!";
        } else if (productDTO.getDescription().isBlank()) {
            message = "Product description can not be empty!";
        } else if (productDTO.getStorageId() <= 0) {
            message = "Invalid storage type. Please select a correct storage!";
        } else if (productDTO.getColorId() <= 0) {
            message = "Invalid color type. Please select a correct color!";
        } else if (productDTO.getQualityId() <= 0) {
            message = "Invalid condition type. Please select a correct condition!";
        } else if (productDTO.getPrice() <= 0) {
            message = "Product price can not be less than or equal to 0";
        } else if (productDTO.getQty() <= 0) {
            message = "Product quantity can not be less than or equal to 0";
        } else {
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                message = "Session expired! Please logged in";
            } else if (httpSession.getAttribute("user") == null) {
                message = "Please logged in";
            } else {
                User sessionUser = (User) httpSession.getAttribute("user");
                Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
                Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user=:user", Seller.class)
                        .setParameter("user", sessionUser)
                        .getSingleResultOrNull();
                if (seller == null) {
                    message = "The requested profile is not a seller account. Please register first as a seller";
                } else {
                    if (!seller.getStatus().getValue().equals(String.valueOf(Status.Type.APPROVED))) {
                        message = "Not approved account. Please be patient till admin approval!";
                    } else {
                        Model model = hibernateSession.find(Model.class, productDTO.getModelId());
                        if (model == null) {
                            message = "Model not found. Please contact administration";
                        } else {
                            Storage storage = hibernateSession.find(Storage.class, productDTO.getStorageId());
                            if (storage == null) {
                                message = "Storage not found. Please contact administration";
                            } else {
                                Color color = hibernateSession.find(Color.class, productDTO.getColorId());
                                if (color == null) {
                                    message = "Color not found. Please contact administration";
                                } else {
                                    Quality quality = hibernateSession.find(Quality.class, productDTO.getQualityId());
                                    if (quality == null) {
                                        message = "Quality not found. Please contact administration";
                                    } else {
                                        Product product = new Product();
                                        product.setTitle(productDTO.getTitle());
                                        product.setDescription(productDTO.getDescription());
                                        product.setModel(model);
                                        product.setStorage(storage);
                                        product.setQuality(quality);
                                        product.setColor(color);
                                        product.setSeller(seller);

                                        Stock stock = new Stock();
                                        stock.setProduct(product);
                                        stock.setPrice(productDTO.getPrice());
                                        stock.setQty(productDTO.getQty());

                                        Discount defaultDiscount = hibernateSession.createNamedQuery("Discount.findDefault", Discount.class)
                                                .getSingleResult();
                                        Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                                                .setParameter("value", String.valueOf(Status.Type.PENDING))
                                                .getSingleResult();
                                        stock.setDiscount(defaultDiscount);
                                        stock.setStatus(pendingStatus);

                                        Transaction transaction = hibernateSession.beginTransaction();
                                        try {
                                            hibernateSession.persist(product);
                                            hibernateSession.persist(stock);
                                            transaction.commit();
                                            status = true;
                                            responseObject.addProperty("productId", product.getId());
                                        } catch (HibernateException e) {
                                            transaction.rollback();
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                hibernateSession.close();
            }
        }
        /// product-inserting-part-end

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
