package com.library.api.controller;

import com.library.api.dto.MemberDTO;
import com.library.api.mapper.MemberMapper;
import com.library.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import org.springframework.http.ResponseEntity;
import java.net.URI;

@RestController
@RequestMapping(value = "/members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Members", description = "Register and view library members")
@ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
public class MemberController {
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    public MemberController(MemberService memberService, MemberMapper memberMapper) {
        this.memberService = memberService;
        this.memberMapper = memberMapper;
    }

    @GetMapping
    @Operation(summary = "List all members")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "List all members - successful response")})
    public List<MemberDTO> getAll() {
        return memberService.getAll().stream().map(memberMapper::toDTO).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Get a member by ID - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public MemberDTO getById(@PathVariable Long id) {
        return memberMapper.toDTO(memberService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Register a member")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Register a member - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")})
    public ResponseEntity<MemberDTO> create(@Valid @RequestBody MemberDTO member) {
        MemberDTO saved = memberMapper.toDTO(memberService.create(memberMapper.toEntity(member)));
        return ResponseEntity.created(URI.create("/members/" + saved.id())).body(saved);
    }
}
