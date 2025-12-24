package lk.jiat.smarttrade.service;

import com.google.gson.JsonObject;
import lk.jiat.smarttrade.entity.Brand;
import lk.jiat.smarttrade.entity.Color;
import lk.jiat.smarttrade.entity.Quality;
import lk.jiat.smarttrade.entity.Storage;
import lk.jiat.smarttrade.util.AppUtil;
import lk.jiat.smarttrade.util.HibernateUtil;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

public class AdvancedSearchService {
    public String getAllProductData(){
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

        hibernateSession.close();

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
