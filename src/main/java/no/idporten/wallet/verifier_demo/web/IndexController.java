package no.idporten.wallet.verifier_demo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<String> demo() {
        return ResponseEntity.ok("demo");
    }

}
