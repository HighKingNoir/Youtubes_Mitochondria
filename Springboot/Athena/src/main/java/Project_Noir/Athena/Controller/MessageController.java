package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.FundChannelRequest;
import Project_Noir.Athena.DTO.UserWithdrawRequest;
import Project_Noir.Athena.Model.Messages;
import Project_Noir.Athena.Repo.MessageRepository;
import Project_Noir.Athena.Repo.UserRepository;
import Project_Noir.Athena.Service.JwtService;
import Project_Noir.Athena.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/Message")
@RequiredArgsConstructor
public class MessageController {
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/All")
    // @dev returns all user payment entities
    public ResponseEntity<List<Messages>> getAllUserMessages(@RequestParam(name = "JWT") String JWT, @RequestParam(name = "start") int start){
        var messageIDs = this.userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow().getMessages();
        Collections.reverse(messageIDs);
        var messages = messageRepository.findAllById(messageIDs.subList(start, Math.min(start + 50, messageIDs.size())));
        Collections.reverse(messages);
        return ResponseEntity.status(HttpStatus.OK).body(messages);
    }

    @GetMapping("/{JWT}/Unread")
    // @dev returns all user payment entities
    public ResponseEntity<List<Messages>> getAllUnreadUserMessages(@PathVariable String JWT){
        return ResponseEntity.status(HttpStatus.OK).body(messageService.getAllUnreadMessages(jwtService.extractUserId(JWT)));
    }

    @GetMapping("View/{messageID}")
    // @dev returns all user payment entities
    public ResponseEntity<Messages> getMessage(@PathVariable String messageID){
        return ResponseEntity.status(HttpStatus.OK).body(messageRepository.findById(messageID).orElseThrow());
    }

    @PatchMapping("/Read/{messageID}")
    // @dev returns all user payment entities
    public ResponseEntity<Object> markAsRead(@PathVariable String messageID){
        var massage = messageRepository.findById(messageID).orElseThrow();
        massage.setHasRead(true);
        messageRepository.save(massage);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/FundChannel")
    public ResponseEntity<String> fundChannel(@RequestBody FundChannelRequest fundChannelRequest, @RequestHeader("Authorization") String jwt){
        messageService.fundChannelMessage(fundChannelRequest, jwtService.getJWTString(jwt));
        return ResponseEntity.status(HttpStatus.OK).body("Channel successfully funded");
    }

    @PostMapping("/User/Withdraw")
    public ResponseEntity<String> userWithdraw(@RequestBody UserWithdrawRequest userWithdrawRequest, @RequestHeader("Authorization") String jwt){
        messageService.userWithdraw(userWithdrawRequest, jwtService.getJWTString(jwt));
        return ResponseEntity.status(HttpStatus.OK).body("Withdrawal successful");
    }
}
