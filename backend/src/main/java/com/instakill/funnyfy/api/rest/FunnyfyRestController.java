package com.instakill.funnyfy.api.rest;

import com.instakill.funnyfy.application.FunnyTextGenerator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FunnyfyRestController {

    private final FunnyTextGenerator funnyTextGenerator;

    public FunnyfyRestController(FunnyTextGenerator funnyTextGenerator) {
        this.funnyTextGenerator = funnyTextGenerator;
    }

    @PostMapping("/funnyfy")
    public FunnyfyResponse funnyfy(@Valid @RequestBody FunnyfyRequest request) {
        return new FunnyfyResponse(funnyTextGenerator.makeFunnier(request.text()));
    }
}
