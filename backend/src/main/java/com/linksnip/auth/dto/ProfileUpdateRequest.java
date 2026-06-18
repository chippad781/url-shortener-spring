package com.linksnip.auth.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(@Size(max = 120) String displayName) {
}
