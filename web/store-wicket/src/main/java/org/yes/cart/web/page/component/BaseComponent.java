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

package org.yes.cart.web.page.component;


import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.yes.cart.web.service.wicketsupport.WicketSupportFacade;
import org.yes.cart.web.support.constants.StorefrontServiceSpringKeys;
import org.yes.cart.web.support.constants.WicketServiceSpringKeys;
import org.yes.cart.web.support.entity.decorator.DecoratorFacade;
import org.yes.cart.web.support.i18n.I18NWebSupport;
import org.yes.cart.web.util.WicketUtil;


public class BaseComponent extends Panel {

    public static final String FEEDBACK = "feedback";

    public static final String HTML_CLASS = "class";
    public static final String HTML_TITLE = "title";
    public static final String HTML_ALT = "alt";
    public static final String HTML_WIDTH = "width";
    public static final String HTML_HEIGHT = "height";


    @SpringBean(name = WicketServiceSpringKeys.WICKET_UTIL)
    private WicketUtil wicketUtil;

    @SpringBean(name = StorefrontServiceSpringKeys.I18N_SUPPORT)
    private I18NWebSupport i18NWebSupport;

    @SpringBean(name = StorefrontServiceSpringKeys.DECORATOR_FACADE)
    private DecoratorFacade decoratorFacade;

    @SpringBean(name = WicketServiceSpringKeys.WICKET_SUPPORT_FACADE)
    private WicketSupportFacade wicketSupportFacade;

    private boolean panelVisible = true;


    /**
     * Construct panel.
     *
     * @param id panel id
     */
    public BaseComponent(final String id) {
        super(id);
    }

    /**
     * Construct panel.
     *
     * @param id    panel id
     * @param model model.
     */
    public BaseComponent(final String id, final IModel<?> model) {
        super(id, model);
    }

    /**
     * @return I18n support object
     */
    public I18NWebSupport getI18NSupport() {
        return i18NWebSupport;
    }

    /**
     * @return decorator facade
     */
    public DecoratorFacade getDecoratorFacade() {
        return decoratorFacade;
    }

    /**
     * @return wicket support facade
     */
    public WicketSupportFacade getWicketSupportFacade() {
        return wicketSupportFacade;
    }

    /**
     * @return utils bean
     */
    public WicketUtil getWicketUtil() {
        return wicketUtil;
    }
}