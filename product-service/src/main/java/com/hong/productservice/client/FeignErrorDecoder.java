package com.hong.productservice.client;

import com.hong.common.exception.ErrorCode;
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
                if(methodKey.contains("getUserById")){
                    return new UserException(ErrorCode.USER_NOT_FOUND);
                }
        }

        return null;
    }
}
