package lk.jiat.smarttrade.controller.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lk.jiat.smarttrade.service.OrderService;
import lk.jiat.smarttrade.util.Env;
import lk.jiat.smarttrade.util.PayHereUtil;

import java.net.URI;

@Path("/payments")
public class PaymentGatewayController {
    @Path("/return")
    @GET
    public Response paymentSuccess(@QueryParam("orderId)") String orderId) {
        return Response.seeOther(
                URI.create(Env.get("app.url") + "/invoice.html?orderId=" + orderId)
        ).build();
    }

    @Path("/cancel")
    @GET
    public Response paymentCancel() {
        System.out.println("cancel");
        return Response.ok().build();
    }

    @POST
    @Path("/notify")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response notify(MultivaluedMap<String, String> form) {

        String orderId = form.getFirst("order_id");
        String statusCode = form.getFirst("status_code");

        if (!PayHereUtil.validateNotify(form)) {
            return Response.status(400).entity("INVALID SIGNATURE").build();
        }
        OrderService orderService = new OrderService();
        if ("2".equals(statusCode)) { // 2 = SUCCESS
            orderService.completeOrder(orderId);
        } else {
            orderService.failOrder(orderId);
        }

        return Response.ok("OK").build();
    }
}
