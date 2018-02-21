package com.backbase.testing.dataloader.setup;

import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.config.functions.FunctionsGetResponseBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBodyParent;
import com.backbase.presentation.accessgroup.rest.spec.v2.accessgroups.serviceagreements.ServiceAgreementGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.LegalEntityByUserGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.UserGetResponseBody;
import com.backbase.testing.dataloader.clients.accessgroup.AccessGroupIntegrationRestClient;
import com.backbase.testing.dataloader.clients.accessgroup.ServiceAgreementsPresentationRestClient;
import com.backbase.testing.dataloader.clients.accessgroup.UserContextPresentationRestClient;
import com.backbase.testing.dataloader.clients.common.LoginRestClient;
import com.backbase.testing.dataloader.clients.legalentity.LegalEntityPresentationRestClient;
import com.backbase.testing.dataloader.clients.user.UserPresentationRestClient;
import com.backbase.testing.dataloader.configurators.AccessGroupsConfigurator;
import com.backbase.testing.dataloader.configurators.ContactsConfigurator;
import com.backbase.testing.dataloader.configurators.LegalEntitiesAndUsersConfigurator;
import com.backbase.testing.dataloader.configurators.MessagesConfigurator;
import com.backbase.testing.dataloader.configurators.PaymentsConfigurator;
import com.backbase.testing.dataloader.configurators.PermissionsConfigurator;
import com.backbase.testing.dataloader.configurators.ProductSummaryConfigurator;
import com.backbase.testing.dataloader.configurators.ServiceAgreementsConfigurator;
import com.backbase.testing.dataloader.configurators.TransactionsConfigurator;
import com.backbase.testing.dataloader.dto.ArrangementId;
import com.backbase.testing.dataloader.dto.CurrencyDataGroup;
import com.backbase.testing.dataloader.dto.UserContext;
import com.backbase.testing.dataloader.dto.UserList;
import com.backbase.testing.dataloader.utils.GlobalProperties;
import com.backbase.testing.dataloader.utils.ParserUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.backbase.testing.dataloader.data.CommonConstants.EXTERNAL_ROOT_LEGAL_ENTITY_ID;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_CONTACTS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_CONVERSATIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_ENTITLEMENTS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_PAYMENTS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_TRANSACTIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_USERS_JSON_LOCATION;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_USERS_WITHOUT_PERMISSIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.SEPA_CT_FUNCTION_NAME;
import static com.backbase.testing.dataloader.data.CommonConstants.USER_ADMIN;
import static com.backbase.testing.dataloader.data.CommonConstants.US_DOMESTIC_WIRE_FUNCTION_NAME;
import static com.backbase.testing.dataloader.data.CommonConstants.US_FOREIGN_WIRE_FUNCTION_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_OK;

public class UsersSetup {

    private GlobalProperties globalProperties = GlobalProperties.getInstance();
    private LoginRestClient loginRestClient = new LoginRestClient();
    private UserPresentationRestClient userPresentationRestClient = new UserPresentationRestClient();
    private ProductSummaryConfigurator productSummaryConfigurator = new ProductSummaryConfigurator();
    private UserContextPresentationRestClient userContextPresentationRestClient = new UserContextPresentationRestClient();
    private AccessGroupIntegrationRestClient accessGroupIntegrationRestClient = new AccessGroupIntegrationRestClient();
    private AccessGroupsConfigurator accessGroupsConfigurator = new AccessGroupsConfigurator();
    private PermissionsConfigurator permissionsConfigurator = new PermissionsConfigurator();
    private ServiceAgreementsConfigurator serviceAgreementsConfigurator = new ServiceAgreementsConfigurator();
    private ServiceAgreementsPresentationRestClient serviceAgreementsPresentationRestClient = new ServiceAgreementsPresentationRestClient();
    private LegalEntityPresentationRestClient legalEntityPresentationRestClient = new LegalEntityPresentationRestClient();
    private TransactionsConfigurator transactionsConfigurator = new TransactionsConfigurator();
    private LegalEntitiesAndUsersConfigurator legalEntitiesAndUsersConfigurator = new LegalEntitiesAndUsersConfigurator();
    private ContactsConfigurator contactsConfigurator = new ContactsConfigurator();
    private PaymentsConfigurator paymentsConfigurator = new PaymentsConfigurator();
    private MessagesConfigurator messagesConfigurator = new MessagesConfigurator();
    private UserList[] userLists = ParserUtil.convertJsonToObject(globalProperties.getString(PROPERTY_USERS_JSON_LOCATION), UserList[].class);

    public UsersSetup() throws IOException {
    }

    public void setupUsersWithAndWithoutFunctionDataGroupsPrivileges() throws IOException {
        if (globalProperties.getBoolean(PROPERTY_INGEST_ENTITLEMENTS)) {
            UserList[] usersWithoutPermissionsLists = ParserUtil.convertJsonToObject(globalProperties.getString(PROPERTY_USERS_WITHOUT_PERMISSIONS), UserList[].class);

            Arrays.stream(userLists).forEach(userList -> {
                List<String> externalUserIds = userList.getExternalUserIds();

                legalEntitiesAndUsersConfigurator.ingestUsersUnderNewLegalEntity(externalUserIds, EXTERNAL_ROOT_LEGAL_ENTITY_ID);

                setupUsersWithAllFunctionDataGroupsAndPrivileges(externalUserIds);
            });

            Arrays.stream(usersWithoutPermissionsLists).parallel().forEach(usersWithoutPermissionsList -> {
                List<String> externalUserIds = usersWithoutPermissionsList.getExternalUserIds();

                legalEntitiesAndUsersConfigurator.ingestUsersUnderNewLegalEntity(externalUserIds, EXTERNAL_ROOT_LEGAL_ENTITY_ID);
            });
        }
    }

    public void setupContactsPerUser() {
        if (globalProperties.getBoolean(PROPERTY_INGEST_CONTACTS)) {
            Arrays.stream(userLists).forEach(userList -> {
                List<String> externalUserIds = userList.getExternalUserIds();

                externalUserIds.forEach(externalUserId -> {
                    loginRestClient.login(externalUserId, externalUserId);
                    userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();
                    contactsConfigurator.ingestContacts();
                });
            });
        }
    }

    public void setupPaymentsPerUser() {
        if (globalProperties.getBoolean(PROPERTY_INGEST_PAYMENTS)) {
            Arrays.stream(userLists).forEach(userList -> {
                List<String> externalUserIds = userList.getExternalUserIds();

                externalUserIds.forEach(externalUserId -> paymentsConfigurator.ingestPaymentOrders(externalUserId));
            });
        }
    }

    public void setupConversationsPerUser() {
        if (globalProperties.getBoolean(PROPERTY_INGEST_CONVERSATIONS)) {
            loginRestClient.login(USER_ADMIN, USER_ADMIN);
            userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();

            Arrays.stream(userLists).forEach(userList -> {
                List<String> externalUserIds = userList.getExternalUserIds();

                externalUserIds.forEach(externalUserId -> messagesConfigurator.ingestConversations(externalUserId));
            });
        }
    }

    public void setupUsersWithAllFunctionDataGroupsAndPrivileges(List<String> externalUserIds) {
        Set<String> internalLegalEntityIds = new HashSet<>();
        List<UserContext> userContextList = new ArrayList<>();
        Map<String, String> serviceAgreementLegalEntityIds = new HashMap<>();
        Map<String, CurrencyDataGroup> serviceAgreementDataGroupIds = new HashMap<>();

        for (String externalUserId : externalUserIds) {
            loginRestClient.login(USER_ADMIN, USER_ADMIN);
            userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();

            internalLegalEntityIds.add(userPresentationRestClient.retrieveLegalEntityByExternalUserId(externalUserId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(LegalEntityByUserGetResponseBody.class)
                .getId());
        }

        internalLegalEntityIds.forEach(internalLegalEntityId -> serviceAgreementsConfigurator.updateMasterServiceAgreementWithExternalIdByLegalEntity(internalLegalEntityId));

        for (String externalUserId : externalUserIds) {
            UserContext userContext = getUserContextBasedOnMasterServiceAgreement(externalUserId);

            userContextList.add(userContext);

            serviceAgreementLegalEntityIds.put(userContext.getExternalServiceAgreementId(), userContext.getExternalLegalEntityId());
        }

        for (Map.Entry<String, String> entry : serviceAgreementLegalEntityIds.entrySet()) {
            String externalServiceAgreementId = entry.getKey();
            String externalLegalEntityId =  entry.getValue();

            CurrencyDataGroup currencyDataGroup = setupArrangementsPerDataGroupForServiceAgreement(externalServiceAgreementId, externalLegalEntityId);

            serviceAgreementDataGroupIds.put(externalServiceAgreementId, currencyDataGroup);
        }

        for (UserContext userContext : userContextList) {
            setupFunctionGroupsAndAssignPermissions(userContext.getExternalUserId(), userContext.getInternalServiceAgreementId(), userContext.getExternalServiceAgreementId(), serviceAgreementDataGroupIds.get(userContext.getExternalServiceAgreementId()), true);
        }
    }

    void setupFunctionGroupsAndAssignPermissions(String externalUserId, String internalServiceAgreementId, String externalServiceAgreementId, CurrencyDataGroup currencyDataGroup, boolean masterServiceAgreement) {
        FunctionsGetResponseBody[] functions = accessGroupIntegrationRestClient.retrieveFunctions()
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(FunctionsGetResponseBody[].class);

        loginRestClient.login(USER_ADMIN, USER_ADMIN);
        userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();

        Arrays.stream(functions).forEach(function -> {
            String functionName = function.getName();
            String functionGroupId;

            if (masterServiceAgreement) {
                functionGroupId = accessGroupsConfigurator.setupFunctionGroupWithAllPrivilegesByFunctionName(internalServiceAgreementId, externalServiceAgreementId, functionName);
            } else {
                functionGroupId = accessGroupsConfigurator.ingestFunctionGroupWithAllPrivilegesByFunctionName(externalServiceAgreementId, functionName);
            }

            permissionsConfigurator.assignPermissions(externalUserId, internalServiceAgreementId, functionName, functionGroupId, currencyDataGroup);
        });
    }

    CurrencyDataGroup setupArrangementsPerDataGroupForServiceAgreement(String externalServiceAgreementId, String externalLegalEntityId) {
        List<ArrangementId> randomCurrencyArrangementIds = new ArrayList<>(productSummaryConfigurator.ingestRandomCurrencyArrangementsByLegalEntityAndReturnArrangementIds(externalLegalEntityId));
        List<ArrangementId> eurCurrencyArrangementIds = new ArrayList<>(productSummaryConfigurator.ingestSpecificCurrencyArrangementsByLegalEntityAndReturnArrangementIds(externalLegalEntityId, ArrangementsPostRequestBodyParent.Currency.EUR));
        List<ArrangementId> usdCurrencyArrangementIds = new ArrayList<>(productSummaryConfigurator.ingestSpecificCurrencyArrangementsByLegalEntityAndReturnArrangementIds(externalLegalEntityId, ArrangementsPostRequestBodyParent.Currency.USD));

        String randomCurrencyDataGroupId = accessGroupsConfigurator.ingestDataGroupForArrangements(externalServiceAgreementId, randomCurrencyArrangementIds);
        String eurCurrencyDataGroupId = accessGroupsConfigurator.ingestDataGroupForArrangements(externalServiceAgreementId, eurCurrencyArrangementIds);
        String usdCurrencyDataGroupId = accessGroupsConfigurator.ingestDataGroupForArrangements(externalServiceAgreementId, usdCurrencyArrangementIds);

        if (globalProperties.getBoolean(PROPERTY_INGEST_TRANSACTIONS)) {
            List<ArrangementId> arrangementIds = new ArrayList<>();

            arrangementIds.addAll(randomCurrencyArrangementIds);
            arrangementIds.addAll(eurCurrencyArrangementIds);
            arrangementIds.addAll(usdCurrencyArrangementIds);

            arrangementIds.parallelStream().forEach(arrangementId -> transactionsConfigurator.ingestTransactionsByArrangement(arrangementId.getExternalArrangementId()));
        }

        return new CurrencyDataGroup()
                .withInternalRandomCurrencyDataGroupId(randomCurrencyDataGroupId)
                .withInternalEurCurrencyDataGroupId(eurCurrencyDataGroupId)
                .withInternalUsdCurrencyDataGroupId(usdCurrencyDataGroupId);
    }

    private UserContext getUserContextBasedOnMasterServiceAgreement(String externalUserId) {
        loginRestClient.login(USER_ADMIN, USER_ADMIN);
        userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();

        String internalUserId = userPresentationRestClient.getUserByExternalId(externalUserId)
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(UserGetResponseBody.class)
            .getId();

        LegalEntityByUserGetResponseBody legalEntity = userPresentationRestClient.retrieveLegalEntityByExternalUserId(externalUserId)
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(LegalEntityByUserGetResponseBody.class);

         String internalServiceAgreementId = legalEntityPresentationRestClient.getMasterServiceAgreementOfLegalEntity(legalEntity.getId())
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(ServiceAgreementGetResponseBody.class)
            .getId();

        String externalServiceAgreementId = serviceAgreementsPresentationRestClient.retrieveServiceAgreement(internalServiceAgreementId)
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(ServiceAgreementGetResponseBody.class)
            .getExternalId();

        return new UserContext()
            .withInternalUserId(internalUserId)
            .withExternalUserId(externalUserId)
            .withInternalServiceAgreementId(internalServiceAgreementId)
            .withExternalServiceAgreementId(externalServiceAgreementId)
            .withInternalLegalEntityId(legalEntity.getId())
            .withExternalLegalEntityId(legalEntity.getExternalId());
    }
}
