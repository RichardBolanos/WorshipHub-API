package com.worshiphub.config

import com.worshiphub.api.auth.*
import com.worshiphub.api.catalog.*
import com.worshiphub.api.chat.*
import com.worshiphub.api.common.PageResponse
import com.worshiphub.api.organization.*
import com.worshiphub.api.scheduling.*
import com.worshiphub.api.system.*
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.context.annotation.Configuration

/**
 * Registers reflection hints for GraalVM native image compilation.
 * All DTO/Request/Response classes must be listed here so that
 * springdoc-openapi can use Kotlin reflection to generate the OpenAPI schema.
 */
@Configuration
@RegisterReflectionForBinding(
    // Auth DTOs
    LoginRequest::class,
    LoginResponse::class,
    UserInfo::class,
    RegisterRequest::class,
    RegisterResponse::class,
    RegisterChurchWithAdminRequest::class,
    ChurchRegistrationResponse::class,
    OAuth2LoginResponse::class,
    SetPasswordRequest::class,
    ChangePasswordRequest::class,
    PasswordStatusResponse::class,
    ForgotPasswordRequest::class,
    ResetPasswordRequest::class,
    ChangeRoleRequest::class,
    UserRoleInfo::class,
    RoleInfo::class,
    ChurchUsersResponse::class,
    AvailableRolesResponse::class,
    SendInvitationRequest::class,
    AcceptInvitationRequest::class,
    InvitationResponse::class,
    InvitationDetailsResponse::class,
    ResendEmailVerificationRequest::class,
    // Catalog DTOs
    CreateSongRequest::class,
    CreateSongResponse::class,
    SongResponse::class,
    UpdateSongRequest::class,
    AddAttachmentRequest::class,
    AttachmentResponse::class,
    AddCommentRequest::class,
    CommentResponse::class,
    CreateCategoryRequest::class,
    CreateCategoryTagRequest::class,
    CategoryResponse::class,
    TagResponse::class,
    // Chat DTOs
    SendChatMessageDto::class,
    SendChatMessageRestDto::class,
    ChatMessageResponseDto::class,
    // Common DTOs
    com.worshiphub.api.common.MessageResponse::class,
    PageResponse::class,
    // Organization DTOs
    CreateTeamRequest::class,
    CreateTeamResponse::class,
    TeamResponse::class,
    ChurchResponse::class,
    TeamMemberResponse::class,
    AssignTeamMemberRequest::class,
    AssignTeamMemberResponse::class,
    RegisterChurchRequest::class,
    UpdateMemberRoleRequest::class,
    UpdateTeamRequest::class,
    UserProfileResponse::class,
    UpdateUserProfileRequest::class,
    com.worshiphub.api.organization.MessageResponse::class,
    UpcomingServiceResponse::class,
    MemberAvailabilityResponse::class,
    UnavailableDateResponse::class,
    TeamSummaryResponse::class,
    // Scheduling DTOs
    CreateSetlistRequest::class,
    GenerateSetlistRequest::class,
    ScheduleServiceRequest::class,
    MemberAssignmentRequest::class,
    ScheduleServiceResponse::class,
    InvitationResponseResponse::class,
    InvitationResponseRequest::class,
    RecurrenceRuleRequest::class,
    MarkUnavailabilityRequest::class,
    AddSongToSetlistRequest::class,
    UpdateSetlistRequest::class,
    // System DTOs
    SystemInfoResponse::class,
)
class NativeImageConfig
