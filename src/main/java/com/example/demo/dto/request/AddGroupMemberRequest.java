package com.example.demo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class AddGroupMemberRequest {

    private UUID memberId;
}
