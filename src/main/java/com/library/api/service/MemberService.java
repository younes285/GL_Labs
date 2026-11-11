package com.library.api.service;

import com.library.api.entity.Member;
import com.library.api.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository members;

    public MemberService(MemberRepository members) {
        this.members = members;
    }

    public List<Member> getAll() {
        return members.findAll();
    }

    public Member getById(Long id) {
        return members.findById(id).orElseThrow(() -> new EntityNotFoundException("Member not found"));
    }

    @Transactional
    public Member create(Member member) {
        member.setId(null);
        return members.save(member);
    }
}
