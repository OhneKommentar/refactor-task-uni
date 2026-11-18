package com.instakill.funnyfy.application;

public interface FunnyTextGenerator {

    /**
     * Rewrites the given text in a funnier style.
     *
     * @throws com.instakill.common.error.ServiceUnavailableException if the text generator cannot be reached
     */
    String makeFunnier(String text);
}
