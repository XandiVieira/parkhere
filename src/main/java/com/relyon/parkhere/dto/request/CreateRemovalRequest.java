package com.relyon.parkhere.dto.request;

import jakarta.validation.constraints.Size;

public record CreateRemovalRequest(@Size(max = 500) String reason) {}
