/*
 * Copyright 2009 Denys Pavlov, Igor Azarnyi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.web.support.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.yes.cart.constants.AttributeNamesKeys;
import org.yes.cart.domain.entity.*;
import org.yes.cart.domain.i18n.I18NModel;
import org.yes.cart.domain.i18n.impl.FailoverStringI18NModel;
import org.yes.cart.domain.i18n.impl.StringI18NModel;
import org.yes.cart.domain.misc.Pair;
import org.yes.cart.service.domain.AttributeService;
import org.yes.cart.service.domain.CustomerService;
import org.yes.cart.service.domain.CustomerWishListService;
import org.yes.cart.service.domain.PassPhrazeGenerator;
import org.yes.cart.shoppingcart.ShoppingCart;
import org.yes.cart.util.ShopCodeContext;
import org.yes.cart.web.support.service.CustomerServiceFacade;

import java.util.*;

/**
 * User: denispavlov
 * Date: 13-10-25
 * Time: 7:03 PM
 */
public class CustomerServiceFacadeImpl implements CustomerServiceFacade {

    private static final String GUEST_TYPE = "B2G";

    private final CustomerService customerService;
    private final CustomerWishListService customerWishListService;
    private final AttributeService attributeService;
    private final PassPhrazeGenerator phrazeGenerator;

    public CustomerServiceFacadeImpl(final CustomerService customerService,
                                     final CustomerWishListService customerWishListService,
                                     final AttributeService attributeService,
                                     final PassPhrazeGenerator phrazeGenerator) {
        this.customerService = customerService;
        this.customerWishListService = customerWishListService;
        this.attributeService = attributeService;
        this.phrazeGenerator = phrazeGenerator;
    }

    /** {@inheritDoc} */
    public boolean isCustomerRegistered(final Shop shop, final String email) {
        return customerService.isCustomerExists(email, shop);
    }

    /** {@inheritDoc} */
    public Customer getCustomerByEmail(final Shop shop, final String email) {
        return customerService.getCustomerByEmail(email, shop);
    }

    /** {@inheritDoc} */
    public Customer getGuestByCart(final Shop shop, final ShoppingCart cart) {
        return customerService.getCustomerByEmail(cart.getGuid(), shop);
    }

    /** {@inheritDoc} */
    public Customer getCheckoutCustomer(final Shop shop, final ShoppingCart cart) {
        if (cart.getLogonState() == ShoppingCart.LOGGED_IN) {
            return getCustomerByEmail(shop, cart.getCustomerEmail());
        }
        return getGuestByCart(shop, cart);
    }

    /** {@inheritDoc} */
    public List<CustomerWishList> getCustomerWishListByEmail(final Shop shop, final String type, final String email,final String visibility, final String... tags) {

        final List<CustomerWishList> allItems = customerWishListService.getWishListByCustomerEmail(email, shop.getShopId());

        final List<CustomerWishList> filtered = new ArrayList<CustomerWishList>();
        for (final CustomerWishList item : allItems) {

            if (visibility != null && !visibility.equals(item.getVisibility())) {
                continue;
            }

            if (type != null && !type.equals(item.getWlType())) {
                continue;
            }

            if (tags != null && tags.length > 0) {
                final String itemTagStr = item.getTag();
                if (StringUtils.isNotBlank(itemTagStr)) {
                    boolean noTag = true;
                    final List<String> itemTags = Arrays.asList(StringUtils.split(itemTagStr, ' '));
                    for (final String tag : tags) {
                        if (itemTags.contains(tag)) {
                            noTag = false;
                            break;
                        }
                    }
                    if (noTag) {
                        continue;
                    }
                }
            } else if (CustomerWishList.SHARED.equals(visibility)) {
                continue; // Do not allow shared lists without tag
            }

            filtered.add(item);

        }

        return filtered;
    }

    /** {@inheritDoc} */
    public void resetPassword(final Shop shop, final Customer customer) {
        customerService.resetPassword(customer, shop, null);
    }

    /** {@inheritDoc} */
    public String registerCustomer(Shop registrationShop, String email, Map<String, Object> registrationData) {

        final Object customerTypeData = registrationData.get("customerType");
        final String customerType = customerTypeData != null ? String.valueOf(customerTypeData) : null;

        final Set<String> types = getShopCustomerTypesCodes(registrationShop, false);
        if (!types.contains(customerType)) {
            ShopCodeContext.getLog(this).warn("SHOP_CUSTOMER_TYPES does not contain '{}' customer type or registrationData does not have 'customerType'", customerType);
            return null;
        }

        final Customer customer = customerService.getGenericDao().getEntityFactory().getByIface(Customer.class);

        customer.setEmail(email);
        customer.setSalutation((String) registrationData.get("salutation"));
        customer.setFirstname((String) registrationData.get("firstname"));
        customer.setLastname((String) registrationData.get("lastname"));
        customer.setMiddlename((String) registrationData.get("middlename"));
        customer.setCustomerType(customerType);

        if (StringUtils.isBlank(customer.getEmail()) ||
                StringUtils.isBlank(customer.getFirstname()) ||
                StringUtils.isBlank(customer.getLastname()) ||
                StringUtils.isBlank(customer.getCustomerType())) {
            ShopCodeContext.getLog(this).warn("Missing required registration data, please check that registration details have sufficient data");
            return null;
        }

        final String password = phrazeGenerator.getNextPassPhrase();
        customer.setPassword(password); // aspect will create hash but we need to generate password to be able to auto-login

        registerCustomerCustomAttributes(customer, customerType, registrationShop, registrationData);

        customerService.create(customer, registrationShop);

        return password; // email is sent via RegistrationAspect
    }

    private void registerCustomerCustomAttributes(final Customer customer, final String customerType, final Shop registrationShop, final Map<String, Object> registrationData) {
        final Map<String, Object> attrData = new HashMap<String, Object>(registrationData);
        attrData.remove("salutation");
        attrData.remove("firstname");
        attrData.remove("lastname");
        attrData.remove("middlename");
        attrData.remove("customerType");
        if (attrData.containsKey("phone") && !attrData.containsKey(AttributeNamesKeys.CUSTOMER_PHONE)) {
            attrData.put(AttributeNamesKeys.CUSTOMER_PHONE, attrData.remove("phone"));
        }

        final List<String> allowed = registrationShop.getSupportedRegistrationFormAttributesAsList(customerType);
        final List<String> allowedFull = new ArrayList<String>();
        allowedFull.addAll(allowed);
        allowedFull.add(AttributeNamesKeys.CUSTOMER_PHONE);

        for (final Map.Entry<String, Object> attrVal : attrData.entrySet()) {

            if (attrVal.getValue() != null &&
                    attrVal.getValue() instanceof String && StringUtils.isNotBlank((String) attrVal.getValue())) {

                if (allowedFull.contains(attrVal.getKey())) {

                    final Attribute attribute = attributeService.findByAttributeCode(attrVal.getKey());

                    if (attribute != null) {

                        final AttrValueCustomer attrValueCustomer = customerService.getGenericDao().getEntityFactory().getByIface(AttrValueCustomer.class);
                        attrValueCustomer.setCustomer(customer);
                        attrValueCustomer.setVal(String.valueOf(attrVal.getValue()));
                        attrValueCustomer.setAttribute(attribute);

                        customer.getAttributes().add(attrValueCustomer);

                    } else {

                        ShopCodeContext.getLog(this).warn("Registration data contains unknown attribute: {}", attrVal.getKey());

                    }

                } else {

                    ShopCodeContext.getLog(this).warn("Registration data contains attribute that is not allowed: {}", attrVal.getKey());

                }

            }

        }
    }


    /** {@inheritDoc} */
    public String registerGuest(Shop registrationShop, String email, Map<String, Object> registrationData) {

        final String customerType = GUEST_TYPE;

        final Set<String> types = getShopCustomerTypesCodes(registrationShop, true);
        if (!types.contains(customerType)) {
            ShopCodeContext.getLog(this).warn("SHOP_CHECKOUT_ENABLE_GUEST is not enabled");
            return null;
        }

        final Customer customer = customerService.getGenericDao().getEntityFactory().getByIface(Customer.class);

        customer.setEmail((String) registrationData.get("cartGuid"));
        customer.setGuest(true);
        customer.setGuestEmail(email);
        customer.setSalutation((String) registrationData.get("salutation"));
        customer.setFirstname((String) registrationData.get("firstname"));
        customer.setLastname((String) registrationData.get("lastname"));
        customer.setMiddlename((String) registrationData.get("middlename"));
        customer.setCustomerType(customerType);

        if (StringUtils.isBlank(customer.getEmail()) ||
                StringUtils.isBlank(customer.getGuestEmail()) ||
                StringUtils.isBlank(customer.getFirstname()) ||
                StringUtils.isBlank(customer.getLastname())) {
            ShopCodeContext.getLog(this).warn("Missing required guest data, please check that registration details have sufficient data");
            return null;
        }

        final Customer existingGuest = customerService.getCustomerByEmail(customer.getEmail(), registrationShop);
        if (existingGuest != null) {
            // All existing guests will be cleaned up by cron job
            existingGuest.setEmail(UUID.randomUUID().toString() + "-expired");
            customerService.update(existingGuest);
        }

        customer.setPassword(UUID.randomUUID().toString()); // make sure we have complex value

        /*
            Below code is for custom attributes, which in theory will not have any effect as this account
            will be deleted after the order is placed.

            However custom implementations may need some custom attributes in checkout process
            so will leave this.
         */

        registerCustomerCustomAttributes(customer, customerType, registrationShop, registrationData);

        customerService.create(customer, registrationShop);

        return customer.getEmail();
    }


    /** {@inheritDoc} */
    public String registerNewsletter(final Shop registrationShop,
                                     final String email,
                                     final Map<String, Object> registrationData) {
        return email; // do nothing, email is sent via NewsletterAspect
    }

    /** {@inheritDoc} */
    public String registerEmailRequest(final Shop registrationShop,
                                       final String email,
                                       final Map<String, Object> registrationData) {
        return email; // do nothing, email is sent via ContactFormAspect
    }

    /** {@inheritDoc} */
    public List<Pair<String, I18NModel>> getShopSupportedCustomerTypes(final Shop shop) {

        final AttrValue av = getShopCustomerTypes(shop);
        if (av != null && StringUtils.isNotBlank(av.getVal())) {

            final String[] types = StringUtils.split(av.getVal(), ',');

            final I18NModel model = new StringI18NModel(av.getDisplayVal());
            final Map<String, String[]> values = new HashMap<String, String[]>();
            for (final Map.Entry<String, String> displayValues : model.getAllValues().entrySet()) {
                values.put(displayValues.getKey(), StringUtils.split(displayValues.getValue(), ','));
            }

            final List<Pair<String, I18NModel>> out = new ArrayList<Pair<String, I18NModel>>(types.length);
            for (int i = 0; i < types.length; i++) {
                final String type = types[i].trim();
                final Map<String, String> names = new HashMap<String, String>();
                for (final Map.Entry<String, String[]> entry : values.entrySet()) {
                    if (entry.getValue().length > i) {
                        names.put(entry.getKey(), entry.getValue()[i]);
                    }
                }
                out.add(new Pair<String, I18NModel>(type, new FailoverStringI18NModel(names, type)));
            }
            return out;
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    public boolean isShopGuestCheckoutSupported(final Shop shop) {
        final String val = shop.getAttributeValueByCode(AttributeNamesKeys.Shop.SHOP_CHECKOUT_ENABLE_GUEST);
        return val != null && Boolean.valueOf(val);
    }

    /** {@inheritDoc} */
    public boolean isShopCustomerTypeSupported(final Shop shop, final String customerType) {
        return getShopCustomerTypesCodes(shop, false).contains(customerType);
    }

    private AttrValue getShopCustomerTypes(Shop shop) {
        return shop.getAttributeByCode(AttributeNamesKeys.Shop.SHOP_CUSTOMER_TYPES);
    }

    private Set<String> getShopCustomerTypesCodes(final Shop shop, final boolean includeGuest) {

        final AttrValue types = getShopCustomerTypes(shop);
        final Set<String> codes = new HashSet<String>();
        if (types != null && StringUtils.isNotBlank(types.getVal())) {
            for (final String code : StringUtils.split(types.getVal(), ',')) {
                codes.add(code.trim());
            }
        }

        if (includeGuest && isShopGuestCheckoutSupported(shop)) {
            codes.add(GUEST_TYPE);
        }
        return codes;
    }

    /** {@inheritDoc} */
    public List<AttrValueCustomer> getShopRegistrationAttributes(final Shop shop, final String customerType) {

        final Set<String> types = getShopCustomerTypesCodes(shop, true);
        if (!types.contains(customerType)) {
            ShopCodeContext.getLog(this).warn("SHOP_CUSTOMER_TYPES does not contain '{}' customer type", customerType);
            return Collections.emptyList();
        }

        final List<String> allowed = shop.getSupportedRegistrationFormAttributesAsList(customerType);
        if (CollectionUtils.isEmpty(allowed)) {
            // must explicitly configure to avoid exposing personal data
            return Collections.emptyList();
        }

        final List<AttrValueCustomer> attrValueCollection = customerService.getRankedAttributeValues(null);
        if (CollectionUtils.isEmpty(attrValueCollection)) {
            return Collections.emptyList();
        }

        final List<AttrValueCustomer> registration = new ArrayList<AttrValueCustomer>();
        final Map<String, AttrValueCustomer> map = new HashMap<String, AttrValueCustomer>(attrValueCollection.size());
        for (final AttrValueCustomer av : attrValueCollection) {
            map.put(av.getAttribute().getCode(), av);
        }
        for (final String code : allowed) {
            final AttrValueCustomer av = map.get(code);
            if (av != null) {
                registration.add(av);
            }
        }

        return registration;  // CPOINT - possibly need to filter some out
    }


    /** {@inheritDoc} */
    public List<Pair<AttrValueCustomer, Boolean>> getCustomerProfileAttributes(final Shop shop, final Customer customer) {

        final List<String> allowed = shop.getSupportedProfileFormAttributesAsList(customer.getCustomerType());
        if (CollectionUtils.isEmpty(allowed)) {
            // must explicitly configure to avoid exposing personal data
            return Collections.emptyList();
        }

        final List<String> readonly = shop.getSupportedProfileFormReadOnlyAttributesAsList(customer.getCustomerType());

        final List<AttrValueCustomer> attrValueCollection = customerService.getRankedAttributeValues(customer);
        if (CollectionUtils.isEmpty(attrValueCollection)) {
            return Collections.emptyList();
        }


        final List<Pair<AttrValueCustomer, Boolean>> profile = new ArrayList<Pair<AttrValueCustomer, Boolean>>();
        final Map<String, AttrValueCustomer> map = new HashMap<String, AttrValueCustomer>(attrValueCollection.size());
        for (final AttrValueCustomer av : attrValueCollection) {
            map.put(av.getAttribute().getCode(), av);
            if ("salutation".equals(av.getAttribute().getCode())) {
                av.setVal(customer.getSalutation());
            } else if ("firstname".equals(av.getAttribute().getCode())) {
                av.setVal(customer.getFirstname());
            } else if ("middlename".equals(av.getAttribute().getCode())) {
                av.setVal(customer.getMiddlename());
            } else if ("lastname".equals(av.getAttribute().getCode())) {
                av.setVal(customer.getLastname());
            }
        }
        for (final String code : allowed) {
            final AttrValueCustomer av = map.get(code);
            if (av != null) {
                profile.add(new Pair<AttrValueCustomer, Boolean>(av, readonly.contains(code)));
            }
        }

        return profile;  // CPOINT - possibly need to filter some out
    }

    /** {@inheritDoc} */
    public void updateCustomer(final Shop shop, final Customer customer) {
        customerService.update(customer);
    }

    /** {@inheritDoc} */
    public void updateCustomerAttributes(final Shop profileShop, final Customer customer, final Map<String, String> values) {

        final List<String> allowed = profileShop.getSupportedProfileFormAttributesAsList(customer.getCustomerType());

        if (CollectionUtils.isNotEmpty(allowed)) {
            // must explicitly configure to avoid exposing personal data
            final List<String> readonly = new ArrayList<String>(profileShop.getSupportedProfileFormReadOnlyAttributesAsList(customer.getCustomerType()));
            // Ensure dummy attributes are not updated
            readonly.addAll(Arrays.asList("salutation", "firstname", "middlename", "lastname"));

            for (final Map.Entry<String, String> entry : values.entrySet()) {

                if (allowed.contains(entry.getKey())) {

                    if (readonly.contains(entry.getKey())) {

                        ShopCodeContext.getLog(this).warn("Profile data contains attribute that is read only: {}", entry.getKey());

                    } else {

                        customerService.addAttribute(customer, entry.getKey(), entry.getValue());

                    }

                } else {

                    ShopCodeContext.getLog(this).warn("Profile data contains attribute that is not allowed: {}", entry.getKey());

                }

            }
        }

        customerService.update(customer);
    }

    /** {@inheritDoc} */
    public String getCustomerPublicKey(final Customer customer) {
        if (StringUtils.isBlank(customer.getPublicKey())) {
            final String phrase = phrazeGenerator.getNextPassPhrase();
            customer.setPublicKey(phrase);
            customerService.update(customer);
        }
        return customer.getPublicKey().concat("-").concat(customer.getLastname());
    }

    /** {@inheritDoc} */
    public Customer getCustomerByPublicKey(final String publicKey) {
        if (StringUtils.isNotBlank(publicKey)) {
            int lastDashPos = publicKey.lastIndexOf('-');
            final String key = publicKey.substring(0, lastDashPos);
            final String lastName = publicKey.substring(lastDashPos + 1);
            return customerService.getCustomerByPublicKey(key, lastName);
        }
        return null;
    }
}
