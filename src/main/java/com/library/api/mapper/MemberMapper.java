package com.library.api.mapper;

import com.library.api.dto.MemberDTO;
import com.library.api.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {
    public MemberDTO toDTO(Member member) {
        return new MemberDTO(member.getId(), member.getName(), member.getEmail(),
                member.getMembershipDate(), member.isActive());
    }

    public Member toEntity(MemberDTO dto) {
        return new Member(dto.id(), dto.name(), dto.email(), dto.membershipDate(), dto.active());
    }
}
