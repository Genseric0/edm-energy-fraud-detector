package com.energy.detection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "O usuário é obrigatório")
    @Size(max = 100)
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(max = 72)
    @ToString.Exclude
    private String password;
}
