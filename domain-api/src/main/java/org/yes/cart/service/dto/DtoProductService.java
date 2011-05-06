package org.yes.cart.service.dto;

import org.yes.cart.domain.dto.ProductSkuDTO;
import org.yes.cart.domain.dto.ProductDTO;
import org.yes.cart.exception.ObjectNotFoundException;
import org.yes.cart.exception.UnableToWrapObjectException;
import org.yes.cart.exception.UnmappedInterfaceException;
import org.yes.cart.exception.UnableToCreateInstanceException;

import java.util.List;

/**
 * Dto Wrapper for product service.
 * <p/>
 * User: dogma
 * Date: Jan 24, 2011
 * Time: 12:32:02 PM
 */
public interface DtoProductService extends GenericDTOService<ProductDTO>, GenericAttrValueService {

    /**
     * Get product sku by code.
     *
     * @param skuCode sku code
     *
     * @return product sku for this sku code
     *
     * @throws ObjectNotFoundException thrown when object is not found
     * @throws org.yes.cart.exception.UnableToWrapObjectException thrown when object cannot be converted to dto
     */
    ProductSkuDTO getProductSkuByCode(String skuCode) throws
                ObjectNotFoundException, UnableToWrapObjectException;

/**
     * Get products, that assigned to given category id.
     * @param categoryId given category id
     * @return List of assined product DTOs
     * @throws org.yes.cart.exception.UnableToCreateInstanceException in case of reflection problem
     * @throws org.yes.cart.exception.UnmappedInterfaceException in case of configuration problem
     */
    List<ProductDTO> getProductByCategory(long categoryId)
            throws UnmappedInterfaceException, UnableToCreateInstanceException;

    /**
     * Get the all products in category.
     *
     * @param categoryId  category id
     * @param firtsResult index of first result
     * @param maxResults  quantity results to return
     * @return list of products
     * @throws org.yes.cart.exception.UnableToCreateInstanceException in case of reflection problem
     * @throws org.yes.cart.exception.UnmappedInterfaceException in case of configuration problem
     */
    List<ProductDTO> getProductByCategoryWithPaging(
            long categoryId,
            int firtsResult,
            int maxResults) throws UnmappedInterfaceException, UnableToCreateInstanceException;

/**
     * Find product by given optional filtering criteria.
     * @param code product code.  use like %%
     * @param name product name.  use like %%
     * @param brandId brand id. use exact match
     * @param productTypeId product type id. use exact match
     * @return list of founded products
     * @throws org.yes.cart.exception.UnableToCreateInstanceException in case of reflection problem
     * @throws org.yes.cart.exception.UnmappedInterfaceException in case of configuration problem
     */
    List<ProductDTO> getProductByConeNameBrandType(
            final String code,
            final String name,
            final long brandId,
            final long productTypeId) throws UnmappedInterfaceException, UnableToCreateInstanceException;


}