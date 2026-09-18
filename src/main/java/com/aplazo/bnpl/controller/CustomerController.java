package com.aplazo.bnpl.controller;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.dto.response.ErrorResponse;
import com.aplazo.bnpl.service.CustomerRegistrationResult;
import com.aplazo.bnpl.service.CustomerService;
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
@RequestMapping("/v1/customers")
@Tag(name = "Customers", description = "Manage customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(
            summary = "Create a customer",
            description = "Allows for customer creation and calculates credit line based on birth date.",
            operationId = "createCustomer"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    headers = {
                            @Header(name = "Location", description = "Relative path to search for newly created customer", schema = @Schema(type = "string", format = "uri-reference")),
                            @Header(name = "X-Auth-Token", description = "JWT with the required roles and required for authentication", schema = @Schema(type = "string", format = "base64"))
                    },
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many request",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerRegistrationResult result = customerService.createCustomer(request);
        CustomerResponse response = result.customerResponse();

        URI location = URI.create("/v1/customers/" + response.id());

        return ResponseEntity.created(location)
                .header("X-Auth-Token", result.token())
                .body(response);
    }

    @Operation(
            summary = "Get customer identified by `customerId`",
            description = "Get customer identified by `customerId`",
            operationId = "getCustomerById",
            security = @SecurityRequirement(name = "aplazoAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))
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
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @Parameter(name = "customerId", description = "Customer's unique identifier", required = true)
            @PathVariable UUID customerId) {
        CustomerResponse customerResponse = customerService.getCustomerByExternalId(customerId);
        return ResponseEntity.ok(customerResponse);
    }
}
