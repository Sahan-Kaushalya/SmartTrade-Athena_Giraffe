async function addToCart(productId, productQty) {
    const response = await fetch("api/user-carts/cart?productId=" + productId + "&qty=" + productQty);

    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            Notiflix.Notify.success(data.message, {position: 'center-top'});
        } else {
            Notiflix.Notify.failure(data.message, {position: 'center-top'});
        }
    } else {
        Notiflix.Notify.failure("Something went wrong. Please try again.", {position: 'center-top'});
    }
}

window.addEventListener("load", async () => {
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
        let cartItemContainer = document.getElementById("cart-item-container");
        if (cartItemContainer) {
            cartItemContainer.innerHTML = "";
        }
        const response = await fetch("api/user-carts/load-cart");
        if (response.ok) {
            const data = await response.json();
            console.log(data);
            if (data.status) {
                Notiflix.Notify.success(data.message, {
                    position: 'center-top'
                });
                if (cartItemContainer) {
                    let subTotal = 0;
                    let totalQty = 0;
                    data.cartList.forEach((cart) => {
                        let total = Number(cart.qty) * parseFloat(cart.price);
                        subTotal += total;
                        totalQty += Number(cart.qty);
                        cartItemContainer.innerHTML += `<tr id="cart-item-row">
                
                            <td class="product-remove"><a class="remove-wishlist" onclick="removeCartItem(${cart.cartId});"><i class="fal fa-times"></i></a>
                            </td>
                            <td class="product-thumbnail"><a href="single-product.html?productId=${cart.stockId}"><img src="${cart.images[0]}"
                                                                           alt="Product"></a></td>
                            <td class="product-title"><a href="#">${cart.title}</a></td>
                            <td class="product-price" data-title="Price"><span class="currency-symbol">Rs. </span><span>${new Intl.NumberFormat("en-US", {
                            minimumFractionDigits: 2
                        }).format(cart.price)}</span>
                            </td>
                            <td class="product-quantity" data-title="Qty">
                                <div class="pro-qty">
                                    <input type="number" class="quantity-input" value="${cart.qty}">
                                </div>
                            </td>
                            <td class="product-subtotal" data-title="Subtotal"><span class="currency-symbol">Rs. </span><span>${new Intl.NumberFormat("en-US", {
                            minimumFractionDigits: 2
                        }).format(total)}</span>
                            </td>
                        </tr>`;
                    });
                    document.getElementById("order-total-quantity").innerHTML = String(totalQty);
                    document.getElementById("order-total-amount").innerHTML = String(new Intl.NumberFormat("en-US", {
                        minimumFractionDigits: 2
                    }).format(subTotal));
                }
                loadSideCartPanel(data.cartList);
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: 'center-top'
                });
            }

        } else {
            Notiflix.Notify.failure("Failed to load cart!", {
                position: 'center-top'
            });
        }
    } catch (error) {
        Notiflix.Notify.failure(error.message, {
            position: 'center-top'
        });
    }
}

function loadSideCartPanel(cartList) {
    const side_panel_cart_item_list = document.getElementById("side-panal-cart-item-list");
    if (side_panel_cart_item_list) {
        side_panel_cart_item_list.innerHTML = "";

        let total = 0;
        let totalQty = 0;
        cartList.forEach(cart => {
            let productSubTotal = cart.price * cart.qty;
            total += productSubTotal;
            totalQty += cart.qty;
            document.getElementById("cart-count").innerHTML = totalQty;
            let cartItem = `<li class="cart-item">
                    <div class="item-img">
                        <a href="single-product.html?productId=${cart.productId}">
                        <img src="${cart.images[0]}" alt=""></a>
                        <button class="close-btn" onclick="removeCartItem(${cart.cartId});"><i class="fas fa-times"></i></button>
                    </div>
                    <div class="item-content">
                        <h3 class="item-title"><a href="#">${cart.title}</a></h3>
                        <div class="item-price"><span class="currency-symbol">Rs. </span>${new Intl.NumberFormat(
                "en-US",
                {minimumFractionDigits: 2})
                .format(cart.price)}</div>
                        <div class="pro-qty item-quantity">
                            <input type="number" class="quantity-input" value="${cart.qty}">
                        </div>
                    </div>
                </li>`;
            side_panel_cart_item_list.innerHTML += cartItem;
        });
        document.getElementById("side-panel-cart-sub-total").innerHTML = new Intl.NumberFormat("en-US",
            {minimumFractionDigits: 2})
            .format(total);
    }
}

async function removeCartItem(cartId) {

}