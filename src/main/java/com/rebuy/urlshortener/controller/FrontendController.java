package com.rebuy.urlshortener.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@Hidden  // ← excludes this controller from Swagger/OpenAPI docs
public class FrontendController {

    @GetMapping(
            value = "/",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Resource> index() {
        Resource resource =
                new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}