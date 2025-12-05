//* Getting product id from url
//  .../smarttrade/single-product.html?productId=1
// */
let params = new URLSearchParams(window.location.search);
const productId = params.get("productId");

// make listener for the onload activity
window.addEventListener("load", async () => {
    try {
        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: '#0284c7'
        });

        // call necessary functions
        await loadSingleProduct();
        await loadSimilarProducts();
    } finally {
        Notiflix.Loading.remove();
    }
});

// make function for the fetch single product data
async function loadSingleProduct() {
    try {
        // make backend request
        const response = await fetch(`api/products/single-product?productId=${productId}`);
        if (response.ok) {// check status 200
            const data = await response.json();
            const product = data.singleProduct;
            product.images.forEach((image, index) => {
                let imgTag = document.getElementById(`image${index + 1}`);
                let thumbImageTag = document.getElementById(`thumb-image${index + 1}`);
                imgTag.src = image;
                thumbImageTag.src = image;
            });
            document.getElementById("product-title").innerHTML = product.title;
            document.getElementById("published-on").innerHTML = product.stockDTOList[0].createdAt;
            document.getElementById("product-price").innerHTML = new Intl.NumberFormat("en-US", {
                minimumFractionDigits: 2
            }).format(product.stockDTOList[0].price);
            document.getElementById("brand-name").innerHTML = product.brandName;
            document.getElementById("model-name").innerHTML = product.modelName;
            document.getElementById("product-quality").innerHTML = product.qualityValue;
            document.getElementById("product-stock").innerHTML = product.stockDTOList[0].qty;
            document.getElementById("color-background").innerHTML = product.colorValue;
            document.getElementById("product-storage").innerHTML = product.storageValue;
            document.getElementById("product-description").innerHTML = product.description;


            const addToCartMain = document.getElementById("add-to-cart-main");
            addToCartMain.addEventListener("click", (evt) => {
                const qty = document.getElementById("add-to-cart-qty");
                addToCart(product.productId, qty.value);// cart.js
                evt.preventDefault();
            })

        } else {
            Notiflix.Notify.failure("Single product data loading failed!", {
                position: 'center-top'
            });

        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });

    }
}

// make function for the fetch related product data

//cloneNode() JavaScript DOM method ekak. Meeka use karanne existing HTML element ekak copy karanna.
// cloneNode() :
// Original element eke duplicate ekak hadai
// Attributes, classes, id, styles copy wenawa
// Child nodes copy karanna nam true denna one
// Child nodes copy karanna epa nam false denna one

async function loadSimilarProducts() {
    try {
        // make request to load similar products
        const response = await fetch(`api/products/similar-products?productId=${productId}`);
        if (response.ok) {
            const data = await response.json();
            let similar_product_main = document.getElementById("smiler-product-main");
            let productHtml = document.getElementById("similer-product");
            similar_product_main.innerHTML = "";
            data.similarProducts.forEach(product => {
                product.stockDTOList.forEach((stock) => {
                    let productCloneHtml = productHtml.cloneNode(true);
                    productCloneHtml.querySelector("#similer-product-a1").href = "single-product.html?productId=" + product.productId;
                    productCloneHtml.querySelector("#similer-product-image").src = product.images[0];
                    productCloneHtml.querySelector("#simler-product-add-to-cart").addEventListener(
                        "click", (e) => {
                            addToCart(product.productId, 1);
                            e.preventDefault();
                        });
                    productCloneHtml.querySelector("#similer-product-a2").href = "single-product.html?id=" + product.id;
                    productCloneHtml.querySelector("#similer-product-title").innerHTML = product.title;
                    productCloneHtml.querySelector("#similer-product-storage").innerHTML = product.storageValue;
                    productCloneHtml.querySelector("#similer-product-price").innerHTML = "Rs. " + new Intl.NumberFormat(
                        "en-US",
                        {minimumFractionDigits: 2})
                        .format(stock.price);
                    productCloneHtml.querySelector("#similer-product-color-border").style.borderColor = "black";
                    productCloneHtml.querySelector("#similer-product-color-background").style.backgroundColor = product.colorValue;

                    // append the clone code
                    similar_product_main.appendChild(productCloneHtml);
                });
            });
            //similer-products-end

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
        } else {
            Notiflix.Notify.failure("Product data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    }
}