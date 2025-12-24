const parms = new URLSearchParams(window.location.search);
const productId = parms.get("productId");

window.addEventListener("load", async () => {
    try {
        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });

        await loadSingleProduct();
        await loadSimilarProducts();
    } finally {
        Notiflix.Loading.remove();
    }

});

async function loadSingleProduct() {
    try {
        const response = await fetch(`api/single-products/product?productId=${productId}`);
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                const product = data.singleProduct;
                product.images.forEach((image, index) => {
                    let mainImg = document.getElementById(`image${index + 1}`);
                    let thumbImg = document.getElementById(`thumb-image${index + 1}`);
                    if (mainImg && thumbImg) {
                        mainImg.src = image;
                        thumbImg.src = image;
                    }
                });
                document.getElementById("product-title").innerHTML = product.title;
                document.getElementById("published-on").innerHTML = product.createdAt;
                document.getElementById("product-price").innerHTML = new Intl.NumberFormat("en-US", {
                    minimumFractionDigits: 2
                }).format(product.price);
                document.getElementById("brand-name").innerHTML = product.brandName;
                document.getElementById("model-name").innerHTML = product.modelName;
                document.getElementById("product-quality").innerHTML = product.qualityValue;
                document.getElementById("product-stock").innerHTML = product.qty;

                // color variation
                document.getElementById("color-border").style.borderColor = "black";
                document.getElementById("color-background").style.backgroundColor = product.colorValue;

                // product storage
                document.getElementById("product-storage").innerHTML = product.storageValue;
                // description
                document.getElementById("product-description").innerHTML = product.description;

                const addToCartBtn = document.getElementById("add-to-cart-main"); // anchor tag -> prevent href
                addToCartBtn.addEventListener("click", async (evt) => {
                    const qtyInput = document.getElementById("add-to-cart-qty");
                    await addToCart(product.stockId, qtyInput.value);
                    evt.preventDefault();
                });
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: 'center-top'
                });
            }
        } else {
            Notiflix.Notify.failure("Single Product data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    }
}

async function loadSimilarProducts() {
    try {
        const response = await fetch(`api/single-products/similar-products?productId=${productId}`);
        if (response.ok) {
            const data = await response.json();
            console.log(data);
            if (data.status) {
                renderingSimilarProducts(data.similarProducts);
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: 'center-top'
                });
            }
        } else {
            Notiflix.Notify.failure("Product data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure("Product data loading failed!", {
            position: 'center-top'
        });

    }
}

function renderingSimilarProducts(productList) {
    const similarProductMain = document.getElementById("similar-product-main");
    similarProductMain.innerHTML = "";
    productList.forEach((product) => {
        similarProductMain.innerHTML += `<div class="slick-single-layout" id="similer-product">
                    <div class="axil-product">
                        <div class="thumbnail">
                            <a href="single-product.html?productId=${product.stockId}" id="similer-product-a1">
                                <img src="${product.images[0]}" id="similer-product-image"
                                     alt="Product Images">
                            </a>

                            <div class="product-hover-action">
                                <ul class="cart-action">
                                    <li class="wishlist"><a href="#"><i class="far fa-heart"></i></a></li>
                                    <li class="select-option"><a id="simler-product-add-to-cart" 
                                    onclick="addToCart(${product.stockId},1)">Add to
                                        Cart</a></li>
                                    <li class="quickview"><a href="#" id="similer-product-a2"><i class="far fa-eye"></i></a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                        <div class="product-content">
                            <div class="inner">
                                <h5 class="title"><a href="#" id="similer-product-title">${product.title}</a>
                                </h5>
                                <p class="b2 mb--10" id="similer-product-storage">${product.storageValue}</p>
                                <div class="product-price-variant">
                                    <span class="price current-price" id="similer-product-price">Rs. ${new Intl.NumberFormat("en-US", {
            minimumFractionDigits: 2
        }).format(product.price)}</span>
                                </div>
                                <div class="color-variant-wrapper">
                                    <ul class="color-variant">
                                        <li class="color-extra-01 active">
                                            <!-- color-border and color-background -->
                                            <span id="similer-product-color-border" style="border-color: black">
                                                        <span class="color" id="similer-product-color-background"
                                                        style="background-color: ${product.colorValue}">
                                                        </span>
                                                    </span>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`;

        $('.recent-product-activation').slick({
            infinite: true,
            slidesToShow: 4,
            slidesToScroll: 4,
            arrows: true,
            dots: false,
            prevArrow: '<button class="slide-arrow prev-arrow"><i class="fal fa-long-arrow-left"></i></button>',
            nextArrow: '<button class="slide-arrow next-arrow"><i class="fal fa-long-arrow-right"></i></button>',
            responsive: [{
                breakpoint: 1199,
                settings: {
                    slidesToShow: 3,
                    slidesToScroll: 3
                }
            },
                {
                    breakpoint: 991,
                    settings: {
                        slidesToShow: 2,
                        slidesToScroll: 2
                    }
                },
                {
                    breakpoint: 479,
                    settings: {
                        slidesToShow: 1,
                        slidesToScroll: 1
                    }
                }
            ]
        });
    })
}