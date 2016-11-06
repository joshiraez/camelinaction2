package camelinaction;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@ConfigurationProperties(prefix = "recommend")
public class RecommendController {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private String cartUrl;
    private String rulesUrl;
    private String ratingsUrl;

    @RequestMapping(value = "recommend", method = RequestMethod.GET, produces = "application/json")
    @SuppressWarnings("unchecked")
    public List<ItemDto> recommend(HttpSession session) {
        String id = session.getId();
        LOG.info("HTTP session id {}", id);

        // get the current item in the shopping cart
        CartDto[] carts = restTemplate.getForObject(cartUrl, CartDto[].class, id);
        // provide details what we have in the shopping cart for the rules
        String cartIds = cartsToCommaString(carts);
        LOG.info("Shopping cart items {}", cartIds);

        LOG.info("Calling rules backend {}", rulesUrl);
        ItemDto[] items = restTemplate.getForObject(rulesUrl, ItemDto[].class, id, cartIds);

        // gather item ids for rating
        String itemIds = itemsToCommaString(items);
        LOG.info("Inventory items to be rated {}", itemIds);

        LOG.info("Calling rating backend {}", ratingsUrl);
        RatingDto[] ratings = restTemplate.getForObject(ratingsUrl, RatingDto[].class, itemIds);

        // append ratings to items
        for (RatingDto rating : ratings) {
            appendRatingToItem(rating, items);
        }

        return Arrays.asList(items);
    }

    public String getCartUrl() {
        return cartUrl;
    }

    public void setCartUrl(String cartUrl) {
        this.cartUrl = cartUrl;
    }

    public String getRulesUrl() {
        return rulesUrl;
    }

    public void setRulesUrl(String rulesUrl) {
        this.rulesUrl = rulesUrl;
    }

    public String getRatingsUrl() {
        return ratingsUrl;
    }

    public void setRatingsUrl(String ratingsUrl) {
        this.ratingsUrl = ratingsUrl;
    }

    private void appendRatingToItem(RatingDto rating, ItemDto[] items) {
        for (ItemDto item : items) {
            if (item.getItemNo() == rating.getItemNo()) {
                item.setRating(rating.getRating());
            }
        }
    }

    private String cartsToCommaString(CartDto[] carts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < carts.length; i++) {
            CartDto cart = carts[i];
            sb.append(cart.getItemId());
            if (i < carts.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private String itemsToCommaString(ItemDto[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            ItemDto item = items[i];
            sb.append(item.getItemNo());
            if (i < items.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}