async function addToCart(productId, productQty){
    const response = await fetch("api/user-carts/cart?productId=" + productId + "&qty=" + productQty);

    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            Notiflix.Notify.success(data.message, { position: 'center-top' });
        } else {
            Notiflix.Notify.failure(data.message, { position: 'center-top' });
        }
    } else {
        Notiflix.Notify.failure("Something went wrong. Please try again.", { position: 'center-top' });
    }
}

window.addEventListener("load", async () => {
    console.log("window loading");
    try {
        Notiflix.Loading.pulse("Loading cart...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });

        await loadCartItems();
    } finally {
        Notiflix.Loading.remove();
    }
});


async function loadCartItems() {
    try {
        const response = await fetch("api/user-carts/load-cart");

        if (!response.ok) {
            Notiflix.Notify.failure("Failed to load cart!");
            return;
        }

        const data = await response.json();
        console.log(data)
        if (data.status) {
            const cart_item_container = document.getElementById("cart-item-container");
            cart_item_container.innerHTML = "";

            let total = 0;
            let totalQty = 0;

            data.cartItems.forEach(cart => {
                let productSubTotal = cart.price * cart.qty;
                total += productSubTotal;
                totalQty += cart.qty;

                let tableData = `<tr id="cart-item-row">
                            <td class="product-remove">
                            <a href="#" class="remove-cart-item" data-cart-id="${cart.cartId}">
                            <i class="fal fa-times"></i>
                            </a></td>
                            <td class="product-thumbnail"><a href="#"><img src="${cart.images[0]}"
                            alt="${cart.title}"></a></td>
                            <td class="product-title">
                            <a href="#">
                            ${cart.title}
                            </a></td>
                            <td class="product-price" data-title="Price"><span class="currency-symbol">Rs. </span><span>
                                ${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(cart.price)}
                            </span></td>
                            <td class="product-quantity" data-title="Qty">
                                <div class="pro-qty">
                                    <input type="number" class="quantity-input" value="${cart.qty}">
                                </div>
                            </td>
                            <td class="product-subtotal" data-title="Subtotal"><span class="currency-symbol">Rs. </span><span>
                                ${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(productSubTotal)}
                            </span></td>
                        </tr>`;
                cart_item_container.innerHTML += tableData;
            });

            document.querySelectorAll(".remove-cart-item").forEach(button => {
                button.addEventListener("click", async function(e) {
                    e.preventDefault(); // prevent # anchor jump

                    const cartId = this.getAttribute("data-cart-id");
                    if (!cartId) return;

                    try {
                        const response = await fetch(`api/products/remove?cartItemId=${cartId}`,
                            { method: "GET" });

                        if (response.ok) {
                            this.closest("tr").remove();
                            Notiflix.Notify.success("Item removed from cart");
                            await loadCartItems();
                        } else {
                            Notiflix.Notify.failure("Failed to remove item");
                        }
                    } catch (error) {
                        Notiflix.Notify.failure("Network error");
                    }
                });
            });


            document.getElementById("order-total-quantity").innerHTML = totalQty;
            document.getElementById("order-total-amount").innerHTML = new Intl.NumberFormat("en-US", {
                minimumFractionDigits: 2
            }).format(total);
        } else {
            Notiflix.Notify.info(data.message || "Your cart is empty.");
        }

    } catch (error) {
        Notiflix.Notify.failure("Something went wrong while loading cart!");
    }
}