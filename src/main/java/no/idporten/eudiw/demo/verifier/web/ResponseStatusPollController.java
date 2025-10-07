package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.service.CacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequiredArgsConstructor
@Slf4j
@Controller
public class ResponseStatusPollController {

    private final CacheService cacheService;
    private final ConfigProvider configProvider;

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

    @GetMapping("/response-result/{type}/{state}")
    public String pollComplete(@PathVariable("type") String type, @PathVariable("state") String state, Model model) {
        MultiValueMap<String, String> claims = cacheService.getState(state);
        model.addAllAttributes(claims);
        model.addAttribute("traces", cacheService.getTrace(state));
        if ("alder".equals(type)) {
            return handleAlder(claims);
        }
        model.addAttribute("claims", claims);
        model.addAttribute("credentialConfig", configProvider.getCredentialConfig(type));
        return "verify-result";
    }

    private static String handleAlder(MultiValueMap<String, String> claims) {
        if (claims.containsKey("age_over_18") && "true".equalsIgnoreCase(claims.getFirst("age_over_18"))) {
            return "alder/over18";
        } else {
            return "alder/under18";
        }
    }

}
