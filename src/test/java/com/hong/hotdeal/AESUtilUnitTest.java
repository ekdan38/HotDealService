package com.hong.hotdeal;

import com.hong.hotdeal.util.AESUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AESUtilUnitTest {

    AESUtil aesUtil = new AESUtil();

    private static final String TEST_SECRET_KEY = "W/Tts3dODgVQ8d+SEd9e3a==";

    @Test
    @DisplayName("AES 암호화")
    public void encrypt() throws Exception {
        //given
        String plainText = "username";

        //when
        String encrypted = aesUtil.encrypt(plainText, TEST_SECRET_KEY);

        //then
        assertThat(plainText).isNotEqualTo(encrypted);
    }

    @Test
    @DisplayName("AES 복호화")
    public void decrypt() throws Exception {
        //given
        String plainText = "username";
        String encrypted = aesUtil.encrypt(plainText, TEST_SECRET_KEY);

        //when
        String decrypted = aesUtil.decrypt(encrypted, TEST_SECRET_KEY);

        //then
        assertThat(decrypted).isEqualTo(plainText);
    }



}