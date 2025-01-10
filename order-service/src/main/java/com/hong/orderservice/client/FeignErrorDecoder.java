package com.hong.orderservice.client;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.common.exception.custom.UserException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()){
            case 400:
                return new RuntimeException(response.reason());
            case 404:
                if(methodKey.contains("getProductsById")){
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                else if(methodKey.contains("decreaseStock")){
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
                }
        }

        return null;
    }
}
