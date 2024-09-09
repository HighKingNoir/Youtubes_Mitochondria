package Project_Noir.Athena.Model;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class Employees {

    @Id
    //The ID of the employee
    private String employeeId;

    //The username of the employee
    private String username;
    //
    private String firstName;
    private String lastName;
    private String password;
    @Email
    private String email;
}
