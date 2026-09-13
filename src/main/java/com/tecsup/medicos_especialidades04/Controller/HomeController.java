package com.tecsup.medicos_especialidades04.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
    @GetMapping("/")
    public String inicio() {
        return "index";   // Busca templates/index.html
    }
}
