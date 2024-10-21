package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.wallet.verifier_demo.service.CacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Controller
public class ResponseStatusPollController {

    private final CacheService cacheService;

    @RequestMapping(method = RequestMethod.GET, path = "/response-status/{type}/{state}")
    public ResponseEntity<String> pollStatus(@PathVariable("type") String type, @PathVariable("state") String state, HttpSession session) {
//        String state = (String) session.getAttribute("state");
        if (state == null) {
            log.warn("No state in session {}", session.getId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
        boolean finished = cacheService.containsState(state) || cacheService.containsCrossDevice(state);
        if (finished) {
            session.removeAttribute("state");
            log.info("Polling finished for state {} in session {}", state, session.getId());
            Boolean crossDevice = cacheService.getCrossDevice(state);
            if(crossDevice != null && !crossDevice){
                return ResponseEntity.status(HttpStatus.OK).body("CLOSE");
            }else{
                return ResponseEntity.status(HttpStatus.OK).body("OK");
            }
        } else {
            log.info("Continue polling for state {} in session {}", state, session.getId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
    }

    // TODO her må det bli mer generisk
    @GetMapping("/response-result/{type}/{state}")
    public String pollComplete(@PathVariable("type") String type, @PathVariable("state") String state, HttpSession session, Model model) {
        Map<String, String> claims = cacheService.getState(state);
        model.addAllAttributes(claims);

        if ("alder".equals(type)) {
            // TODO kanskje modellen skulle fikse dette selv
            if (claims.getOrDefault("age_over_18", "false").equals("true")) {
                return "alder/over18";
            } else {
                return "alder/under18";
            }
        }
        return "fullmakt/result";
    }

}
