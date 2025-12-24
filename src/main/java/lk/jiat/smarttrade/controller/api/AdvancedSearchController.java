package lk.jiat.smarttrade.controller.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.smarttrade.service.AdvancedSearchService;

@Path("/advanced-search")
public class AdvancedSearchController {
    private final AdvancedSearchService advancedSearchService = new AdvancedSearchService();

    @Path("/all-data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadAdvancedSearchData() {
        String responseJson = advancedSearchService.getAllProductData();
        return Response.ok().entity(responseJson).build();
    }
}
