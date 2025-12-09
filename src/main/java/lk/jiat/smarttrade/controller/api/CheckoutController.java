package lk.jiat.smarttrade.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.smarttrade.dto.CheckoutRequestDTO;
import lk.jiat.smarttrade.service.CheckoutService;
import lk.jiat.smarttrade.util.AppUtil;

@Path("/checkout")
public class CheckoutController {

    @GET
    @Path("/load-checkout-data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadCheckoutData(@Context HttpServletRequest request) {
        String responseJson = new CheckoutService().loadCheckoutData(request);
        return Response.ok().entity(responseJson).build();

    }


    @POST
    @Path("/checkout-process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processCheckout(String jsonData, @Context HttpServletRequest request) {
        CheckoutRequestDTO checkoutDTO = AppUtil.GSON.fromJson(jsonData, CheckoutRequestDTO.class);
        String responseJson = new CheckoutService().processCheckout(checkoutDTO, request);

        return Response.ok().entity(responseJson).build();
    }
}
