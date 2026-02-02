package com.example.mroads.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {
	 @GetMapping("/test")
	    public String test() {
	        return "MOVE backend is working";
	    }
}
