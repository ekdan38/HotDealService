package com.hong.hotdeal.web.controller.auth;

import com.hong.hotdeal.service.ReissueService;
import com.hong.hotdeal.web.dto.response.ResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReissueController {

    private final ReissueService reissueService;

    @PostMapping("/reissue")
    private ResponseEntity<ResponseDto<?>> reissue(HttpServletRequest request, HttpServletResponse response){

        String result = reissueService.reissueToken(request, response);
        ResponseDto<?> responseDto = new ResponseDto<>(result);
        return ResponseEntity.ok().body(responseDto);
    }


}
