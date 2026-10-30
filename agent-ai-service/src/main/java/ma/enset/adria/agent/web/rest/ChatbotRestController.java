package ma.enset.adria.agent.web.rest;

import ma.enset.adria.agent.service.BankAiAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatbotRestController {

    private final BankAiAgent bankAiAgent;

    public ChatbotRestController(BankAiAgent bankAiAgent) {
        this.bankAiAgent = bankAiAgent;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String askGet(
            @RequestParam(name = "query", defaultValue = "Donne moi la liste des comptes bancaires") String query,
            @RequestParam(name = "conversationId", defaultValue = "default") String conversationId
    ) {
        return bankAiAgent.chat(query, conversationId);
    }

    @PostMapping
    public Map<String, String> askPost(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "Donne moi la liste des comptes bancaires");
        String conversationId = request.getOrDefault("conversationId", "default");
        return Map.of(
                "query", query,
                "answer", bankAiAgent.chat(query, conversationId)
        );
    }

    @GetMapping("/tools")
    public List<Map<String, String>> listMcpTools() {
        return bankAiAgent.getRegisteredMcpTools();
    }
}
