async function addToCart(stockId, qty) {
    try {
        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });
        const response = await fetch(`api/carts/add-to-cart?sId=${stockId}&qty=${qty}`);
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Notify.success(data.message, {
                    position: 'center-top'
                });
                await loadCartItems();
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: 'center-top'
                });
            }
        } else {
            Notiflix.Notify.failure("Add to cart process failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });

    } finally {
        Notiflix.Loading.remove();
    }
}

async function loadCartItems() {
    try {
        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });

        const response = await fetch("api/carts/all-carts");
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                console.log(data);
                Notiflix.Notify.success(data.message, {
                    position: 'center-top'
                });
                renderingMainPanel(data.cartItems);
                renderingSidePanel(data.cartItems);
            } else {
                Notiflix.Notify.info(data.message, {
                    position: 'center-top'
                });
            }
        } else {
            Notiflix.Notify.failure("Cart items loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    } finally {
        Notiflix.Loading.remove();
    }
}

function renderingMainPanel(cartItems) {
    const cartItemContainer = document.getElementById("cart-item-container");
    if (cartItemContainer) {
        cartItemContainer.innerHTML = "";

        let total = 0;
        let totalQty = 0;

        cartItems.forEach((cart) => {
            let itemsTotal = parseFloat(cart.price) * parseInt(cart.qty);
            total += itemsTotal;
            totalQty += parseInt(cart.qty);
            cartItemContainer.innerHTML += `<tr id="cart-item-row">
                            <td class="product-remove"><a onclick="removeCartItem(${cart.cartId});" class="remove-wishlist"><i class="fal fa-times"></i></a>
                            </td>
                            <td class="product-thumbnail"><a href="single-product.html?productId=${cart.stockId}"><img src="${cart.images[0]}"
                                                                           alt="Product"></a></td>
                            <td class="product-title"><a href="#">${cart.productTitle}</a></td>
                            <td class="product-price" data-title="Price"><span class="currency-symbol">Rs. </span>
                            <span>${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(cart.price)}</span>
                            </td>
                            <td class="product-quantity" data-title="Qty">
                                <div class="pro-qty">
                                    <input type="number" class="quantity-input" value="${cart.qty}">
                                </div>
                            </td>
                            <td class="product-subtotal" data-title="Subtotal"><span class="currency-symbol">Rs. </span>
                            <span>${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(itemsTotal)}</span>
                            </td>
                        </tr>`;
        });
        document.getElementById("order-total-quantity").innerHTML = totalQty;
        document.getElementById("order-total-amount").innerHTML = new Intl.NumberFormat("en-US",
            {minimumFractionDigits: 2}).format(total);

    }
}

function renderingSidePanel(cartItems) {
    const side_panel_cart_item_list = document.getElementById("side-panal-cart-item-list");
    if (side_panel_cart_item_list) {
        side_panel_cart_item_list.innerHTML = "";
        let total = 0;
        let totalQty = 0;
        cartItems.forEach(cart => {
            let productSubTotal = cart.price * cart.qty;
            total += productSubTotal;
            totalQty += cart.qty;
            let cartItem = `<li class="cart-item">
                    <div class="item-img">
                        <a href="single-product.html?productId=${cart.stockId}">
                        <img src="${cart.images[0]}" alt=""></a>
                        <button class="close-btn" onclick="removeCartItem(${cart.cartId});"><i class="fas fa-times"></i></button>
                    </div>
                    <div class="item-content">
                        <h3 class="item-title"><a href="#">${cart.productTitle}</a></h3>
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
        document.getElementById("cart-count").innerHTML = totalQty;
    }
}

async function removeCartItem(cartId) {
    try {
        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });

        const response = await fetch(`api/carts/remove-cart/${cartId}`, {
            method: "DELETE"
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Notify.success(data.message, {
                    position: 'center-top'
                });
                window.location.reload();
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: 'center-top'
                });
            }
        } else {
            Notiflix.Notify.failure("Cart item removing failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    } finally {
        Notiflix.Loading.remove();
    }
}