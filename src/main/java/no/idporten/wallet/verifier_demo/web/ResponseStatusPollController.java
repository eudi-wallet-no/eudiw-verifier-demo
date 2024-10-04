package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.idporten.wallet.verifier_demo.service.CacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@RequiredArgsConstructor
@Controller
public class ResponseStatusPollController {

    private final CacheService cacheService;

    @RequestMapping(method = RequestMethod.GET, path = "/response-status")
    public ResponseEntity<String> status(HttpSession session) {
        String state = (String) session.getAttribute("state");
        if (state == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
        boolean finished = cacheService.containsState(state);
        return finished ?
                ResponseEntity.status(HttpStatus.OK).body("OK") :
                ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
    }

    @GetMapping("/response-result")
    @ResponseBody
    public String result(HttpSession session, Model model) {
        String state = (String) session.getAttribute("state");
        Map<String, String> claims = cacheService.getState(state);
        model.addAllAttributes(claims);
        return claims.toString();
    }

}
