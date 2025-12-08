window.addEventListener("load", async () => {
    Notiflix.Loading.pulse("Wait...", {
        clickToClose: false,
        svgColor: '#0284c7'
    });
    try {
        await loadSearchData();
    } finally {
        Notiflix.Loading.remove();
    }
});

async function loadSearchData() {
    try {
        const response = await fetch("api/data/product-data");
        if (response.ok) {
            const data = await response.json();
            console.log(data);
            renderingOptions("brand", data.brandList, "name");
            renderingOptions("condition", data.qualityList, "value");
            renderingOptions("color", data.colorList, "value");
            renderingOptions("storage", data.storageList, "value");
            updateProductView(data);
        } else {
            Notiflix.Notify.failure("Data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    }
}

// prefix for the grab component -> brand-options, condition-option (prefix-options)
// grab the li component -> brand-li, condition-li (prefix-li)

// data list is the response list (product data)
// property for the database column name (brand, storage, color, quality)
function renderingOptions(prefix, dataList, property) {
    let options = document.getElementById(prefix + "-options");
    let li = document.getElementById(prefix + "-li");
    options.innerHTML = "";

    dataList.forEach(item => {
        let li_clone = li.cloneNode(true);
        if (prefix === "color") {
            li_clone.style.borderColor = "black";
            li_clone.querySelector("#" + prefix + "-a").style.backgroundColor = item[property];
        } else {
            li_clone.querySelector("#" + prefix + "-a").innerHTML = item[property];
        }
        options.appendChild(li_clone);
    });

    const all_li = document.querySelectorAll("#" + prefix + "-options li");
    all_li.forEach(list => {
        list.addEventListener("click", function () {
            all_li.forEach(y => {
                y.classList.remove("chosen"); // <li class=".."><a>...</a></l>
            });
            this.classList.add("chosen");// <li class="choosen .."><a>...</a></l>
        });
    });
}

const st_product = document.getElementById("st-product"); // product card parent node
let st_pagination_button = document.getElementById("st-pagination-button");
let current_page = 0;

function updateProductView(data) {
    const product_container = document.getElementById("st-product-container");
    product_container.innerHTML = "";
    data.productList.forEach(product => {
        let st_product_clone = st_product.cloneNode(true);// enable child nodes cloning / allow child nodes
        st_product_clone.querySelector("#st-product-a-1").href = "single-product.html?productId=" + product.productId;
        st_product_clone.querySelector("#st-product-img-1").src = product.images[0];
        st_product_clone.querySelector("#st-product-add-to-cart").addEventListener(
            "click", async (e) => {
                await addToCart(product.productId, 1);
                e.preventDefault();
            });
        st_product_clone.querySelector("#st-product-a-2").href = "single-product.html?productId=" + product.productId;
        st_product_clone.querySelector("#st-product-title-1").innerHTML = product.title;
        st_product_clone.querySelector("#st-product-price-1").innerHTML = new Intl.NumberFormat(
            "en-US",
            {minimumFractionDigits: 2})
            .format(product.price);

        //append child
        product_container.appendChild(st_product_clone);
    });

    let st_pagination_container = document.getElementById("st-pagination-container");
    st_pagination_container.innerHTML = "";
    let all_product_count = data.allProductCount;
    document.getElementById("all-item-count").innerHTML = all_product_count;
    let product_per_page = 6;
    let pages = Math.ceil(all_product_count / product_per_page); // round upper integer

    //previous-button
    if (current_page !== 0) {
        let st_pagination_button_prev_clone = st_pagination_button.cloneNode(true);
        st_pagination_button_prev_clone.innerHTML = "Prev";
        st_pagination_button_prev_clone.addEventListener(
            "click", async (e) => {
                current_page--;
                await searchProduct(current_page * product_per_page);
                e.preventDefault();
            });
        st_pagination_container.appendChild(st_pagination_button_prev_clone);
    }


    // pagination-buttons
    for (let i = 0; i < pages; i++) {
        let st_pagination_button_clone = st_pagination_button.cloneNode(true);
        st_pagination_button_clone.innerHTML = i + 1;
        st_pagination_button_clone.addEventListener(
            "click", async (e) => {
                current_page = i;
                await searchProduct(i * product_per_page);
                e.preventDefault();
            });

        if (i === Number(current_page)) {
            st_pagination_button_clone.className = "axil-btn btn btn-primary btn-lg fw-bold ml--10";
        } else {
            st_pagination_button_clone.className = "axil-btn btn btn-outline-secondary btn-lg ml--10";
        }
        st_pagination_container.appendChild(st_pagination_button_clone);
    }

    // next-button
    if (current_page !== (pages - 1)) {
        let st_pagination_button_next_clone = st_pagination_button.cloneNode(true);
        st_pagination_button_next_clone.innerHTML = "Next";
        st_pagination_button_next_clone.addEventListener(
            "click", async (e) => {
                current_page++;
                await searchProduct(current_page * product_per_page);
                e.preventDefault();
            });
        st_pagination_container.appendChild(st_pagination_button_next_clone);
    }
}

async function searchProduct(firstResult) {
    Notiflix.Loading.pulse("Wait...", {
        clickToClose: false,
        svgColor: '#0284c7'
    });
    const brand_name = document.getElementById("brand-options")
        .querySelector(".chosen")?.querySelector("a").innerHTML; // ? - optional changing > access if exists

    const condition_name = document.getElementById("condition-options")
        .querySelector(".chosen")?.querySelector("a").innerHTML;

    const color_name = document.getElementById("color-options")
        .querySelector(".chosen")?.querySelector("a").style.backgroundColor;

    const storage_value = document.getElementById("storage-options")
        .querySelector(".chosen")?.querySelector("a").innerHTML;

    const price_range_start = $("#slider-range").slider("values", 0); //left
    const price_range_end = $("#slider-range").slider("values", 1);//right

    const sort_value = document.getElementById("st-sort").value;

    const data = {
        firstResult: firstResult,
        brandName: brand_name,
        conditionName: condition_name,
        colorName: color_name,
        storageValue: storage_value,
        priceStart: price_range_start,
        priceEnd: price_range_end,
        sortValue: sort_value
    };

    const dataJSON = JSON.stringify(data);
    try {
        const response = await fetch("api/products/advanced-search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: dataJSON
        });
        if (response.ok) {
            const data = await response.json();
            updateProductView(data);
        } else {
            Notiflix.Notify.failure("Product data loading failed!", {
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