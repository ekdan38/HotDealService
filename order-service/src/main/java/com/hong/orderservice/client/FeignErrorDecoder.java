package com.hong.orderservice.client;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()){
            case 400:
                return FeignException.errorStatus(methodKey, response, null, null);
            case 404:
                if(methodKey.contains("getProductsById")){
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                else if(methodKey.contains("decreaseStock")){
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                else if(methodKey.contains("increaseStock")){
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                else{
                    return new ResponseStatusException(HttpStatusCode.valueOf(response.status()));
                }
            default:
                return FeignException.errorStatus(methodKey, response, null, null);
        }
    }
}
