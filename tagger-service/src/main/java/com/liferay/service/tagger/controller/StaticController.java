package com.liferay.service.tagger.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class StaticController {

	@RequestMapping("/test")
    public String index() {
        return "Greetings from Spring Boot!";
    }
	
}
