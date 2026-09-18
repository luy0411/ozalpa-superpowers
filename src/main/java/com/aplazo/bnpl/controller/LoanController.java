package com.aplazo.bnpl.controller;

import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.ErrorResponse;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/loans")
@Tag(name = "Loans", description = "Manage loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(
            summary = "Create a loan",
            description = "Creates a new loan for a customer, generates payment plan installments, and updates available credit line.",
            operationId = "createLoan",
            security = @SecurityRequirement(name = "aplazoAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    headers = {
                            @Header(name = "Location", description = "Relative path to search for newly created loan", schema = @Schema(type = "string", format = "uri-reference"))
                    },
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody LoanRequest request) {
        LoanResponse response = loanService.createLoan(request);
        URI location = URI.create("/v1/loans/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Get loan identified by `loanId`",
            description = "Get loan identified by `loanId`",
            operationId = "getLoanById",
            security = @SecurityRequirement(name = "aplazoAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getLoanById(
            @Parameter(name = "loanId", description = "Loan's unique identifier", required = true)
            @PathVariable UUID loanId) {
        LoanResponse response = loanService.getLoanByExternalId(loanId);
        return ResponseEntity.ok(response);
    }
}
