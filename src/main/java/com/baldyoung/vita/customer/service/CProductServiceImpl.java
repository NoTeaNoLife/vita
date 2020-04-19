package com.baldyoung.vita.customer.service;

import com.baldyoung.vita.common.dao.ProductDao;
import com.baldyoung.vita.common.pojo.dto.product.CProductDto;
import com.baldyoung.vita.common.pojo.entity.ProductEntity;
import com.baldyoung.vita.merchant.service.ProductSortServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.baldyoung.vita.common.utility.CommonMethod.toInteger;

@Service
public class CProductServiceImpl {
    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductSortServiceImpl productSortService;

    @Autowired
    private CShoppingCartServiceImpl cShoppingCartService;

    public List<CProductDto> getValidProductForProductType(Integer productTypeId, Integer roomId) {
        ProductEntity entity = new ProductEntity();
        entity.setProductTypeId(productTypeId);
        entity.setProductIsShow(1);
        List<ProductEntity> list = productDao.selectProductWithCondition(entity);
        List<CProductDto> result = new ArrayList(list.size());
        if (null != list) {
            Map currentQuantityMap = cShoppingCartService.getALLItemFromShoppingCart(roomId);
            for (ProductEntity cell : list) {
                CProductDto dto = new CProductDto(cell);
                // 获取商品排序值
                dto.setProductGrade(productSortService.getProductGradeByProductId(cell.getProductId()));
                // 获取当前商品在购物车中的数量
                Integer currentQuantity = toInteger(currentQuantityMap.get(String.valueOf(dto.getProductId())));
                dto.setCurrentQuantity(currentQuantity);
                result.add(dto);
            }
        }
        return result;
    }
}
