package lk.jiat.smarttrade.controller.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/payments")
public class PaymentGatewayController {
    @Path("/return")
    @GET
    public Response paymentSuccess() {
        System.out.println("return");
        return Response.ok().entity("{name:'Anjana'}").build();
    }

    @Path("/cancel")
    @GET
    public Response paymentCancel() {
        System.out.println("cancel");
        return Response.ok().build();
    }

    @Path("/notify")
    @GET
    public Response paymentNotify() {
        System.out.println("notify");
        return Response.ok().build();
    }
}
