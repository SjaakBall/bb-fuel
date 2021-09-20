package com.backbase.ct.bbfuel.client.productsummary;

import static com.backbase.ct.bbfuel.util.ResponseUtils.isBadRequestExceptionWithErrorKey;
import static org.apache.http.HttpStatus.SC_CREATED;

import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.config.BbFuelConfiguration;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.ArrangementAddedResponse;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.BalanceHistoryItem;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.PostArrangement;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.ProductItem;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.Subscription;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArrangementsIntegrationRestClient extends RestClient {

    private final BbFuelConfiguration config;

    private static final String SERVICE_VERSION = "v2";
    private static final String ENDPOINT_ARRANGEMENTS = "/arrangements";
    private static final String ENDPOINT_PRODUCTS = "/products";
    private static final String ENDPOINT_BALANCE_HISTORY = "/balance-history";
    private static final String ENDPOINT_SUBSCRIPTION = "/subscriptions";

    @PostConstruct
    public void init() {
        setBaseUri(config.getDbs().getArrangements());
        setVersion(SERVICE_VERSION);
    }

    public ArrangementAddedResponse ingestArrangement(PostArrangement body) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .post(getPath(ENDPOINT_ARRANGEMENTS))
            .then()
            .statusCode(SC_CREATED)
            .extract()
            .as(ArrangementAddedResponse.class);
    }

    public Response ingestParentPocketArrangement(PostArrangement body) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .post(getPath(ENDPOINT_ARRANGEMENTS));
    }

    /**
     * Ingest parent pocket arrangement when not already existing
     *
     * @param arrangement the parent pocket arrangement
     * @return ArrangementAddedResponse
     */
    public ArrangementAddedResponse ingestParentPocketArrangementAndLogResponse(PostArrangement arrangement) {
        ArrangementAddedResponse arrangementsPostResponseBody = null;
        Response response = ingestParentPocketArrangement(arrangement);
        if (isBadRequestExceptionWithErrorKey(response, "arrangements.api.alreadyExists.arrangement")) {
            log.info("Arrangement [{}] already exists, skipped ingesting this arrangement", arrangement.getProductId());
        } else {
            log.info("Arrangement [{}] ingested", arrangement.getId());
            arrangementsPostResponseBody = response.then()
                .statusCode(SC_CREATED)
                .extract()
                .as(ArrangementAddedResponse.class);
        }
        return arrangementsPostResponseBody;
    }

    public void ingestProductAndLogResponse(ProductItem product) {
        Response response = ingestProduct(product);

        if (isBadRequestExceptionWithErrorKey(response, "account.api.product.alreadyExists")) {
            log.info("Product [{}] already exists, skipped ingesting this product", product.getProductKindName());
        } else if (response.statusCode() == SC_CREATED) {
            log.info("Product [{}] ingested", product.getProductKindName());
        } else {
            response.then()
                .statusCode(SC_CREATED);
        }
    }

    public Response ingestBalance(BalanceHistoryItem balanceHistoryPostRequestBody) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(balanceHistoryPostRequestBody)
            .post(getPath(ENDPOINT_BALANCE_HISTORY));
    }

    private Response ingestProduct(ProductItem body) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .post(getPath(ENDPOINT_PRODUCTS));
    }

    public Response postSubscriptions(String externalArrangementId, Subscription subscription) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(subscription)
            .post(getPath(ENDPOINT_ARRANGEMENTS) + "/" + externalArrangementId + ENDPOINT_SUBSCRIPTION);
    }
}
