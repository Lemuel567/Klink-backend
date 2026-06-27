
package com.example.demo.dto.request;

import com.example.demo.model.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProjectStatusRequest {

    @NotNull(message = "Status is required")
    private ProjectStatus status;

    @Size(max = 1000)
    private String reason;
}