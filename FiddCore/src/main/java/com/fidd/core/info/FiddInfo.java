package com.fidd.core.info;

import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddInfo.class)
@JsonDeserialize(as = ImmutableFiddInfo.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public interface FiddInfo {
    @Nullable SearchInfo searchInfo();
    @Nullable BasicInfo basicInfo();
    @Nullable Authorship authorship();
    @Nullable Affiliation affiliation();
    @Nullable Donations donations();

    @Value.Immutable
    @JsonSerialize(as = ImmutableSearchInfo.class)
    @JsonDeserialize(as = ImmutableSearchInfo.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface SearchInfo {
        @Nullable String name();
        @Nullable String description();
        @Nullable List<String> tags();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBasicInfo.class)
    @JsonDeserialize(as = ImmutableBasicInfo.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface BasicInfo {
        @Nullable String fiddCertificate();
        @Nullable String subtitle();
        @Nullable String primaryTopic();
        @Nullable List<String> subTopics();
        @Nullable String language();
        @Nullable String timezone();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableAuthorship.class)
    @JsonDeserialize(as = ImmutableAuthorship.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Authorship {
        @Nullable Owner owner();
        @Nullable Boolean allowGuestAuthors();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableOwner.class)
    @JsonDeserialize(as = ImmutableOwner.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Owner {
        @Nullable String name();
        @Nullable String email();
        @Nullable String bio();
        @Nullable Map<String, String> socialLinks();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableAffiliation.class)
    @JsonDeserialize(as = ImmutableAffiliation.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Affiliation {
        @Nullable List<String> affiliateDisclaimer();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableDonations.class)
    @JsonDeserialize(as = ImmutableDonations.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Donations {
        @Nullable String patreonUrl();
        @Nullable String buyMeACoffeeUrl();
        @Nullable Map<String, String> other();
    }

    // Cool ideas that can be incorporated in our service, but don't quite fit the purpose of this document below:

    /*
    Branding branding();
    SeoAndMeta seoAndMeta();
    Settings settings();
    Integrations integrations();
    Legal legal();
    MetricsSnapshot metricsSnapshot();
    Audit audit();
    */

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableBranding.class)
    @JsonDeserialize(as = ImmutableBranding.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Branding {
        Optional<String> logoUrl();
        Optional<String> faviconUrl();
        Optional<String> coverImageUrl();
        Theme theme();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableTheme.class)
    @JsonDeserialize(as = ImmutableTheme.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Theme {
        String mode();
        Colors colors();
        Typography typography();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableColors.class)
    @JsonDeserialize(as = ImmutableColors.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Colors {
        String primary();
        String secondary();
        String backgroundLight();
        String backgroundDark();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableTypography.class)
    @JsonDeserialize(as = ImmutableTypography.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Typography {
        String headingsFont();
        String bodyFont();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableSeoAndMeta.class)
    @JsonDeserialize(as = ImmutableSeoAndMeta.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface SeoAndMeta {
        String metaTitle();
        String metaDescription();
        List<String> keywords();
        String canonicalUrl();
        Optional<String> robotsTxtCustomization();
        Map<String, String> openGraphDefaults();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableSettings.class)
    @JsonDeserialize(as = ImmutableSettings.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Settings {
        String visibility();
        int postsPerPage();
        boolean enableRssFeed();
        Optional<String> rssFeedUrl();
        Comments comments();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableMonetization.class)
    @JsonDeserialize(as = ImmutableMonetization.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Monetization {
        boolean adsEnabled();
        Optional<String> adsensePublisherId();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableComments.class)
    @JsonDeserialize(as = ImmutableComments.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Comments {
        boolean enabled();
        String provider();
        boolean requireModeration();
        boolean allowAnonymous();
        List<String> bannedWordsList();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableIntegrations.class)
    @JsonDeserialize(as = ImmutableIntegrations.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Integrations {
        Analytics analytics();
        Newsletter newsletter();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableAnalytics.class)
    @JsonDeserialize(as = ImmutableAnalytics.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Analytics {
        Optional<String> googleAnalyticsId();
        Optional<String> plausibleDomain();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableNewsletter.class)
    @JsonDeserialize(as = ImmutableNewsletter.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Newsletter {
        String provider();
        Optional<String> signupFormUrl();
        boolean popupEnabled();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableLegal.class)
    @JsonDeserialize(as = ImmutableLegal.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Legal {
        String copyrightNotice();
        Optional<String> privacyPolicyUrl();
        Optional<String> termsOfServiceUrl();
        boolean cookieConsentRequired();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableMetricsSnapshot.class)
    @JsonDeserialize(as = ImmutableMetricsSnapshot.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface MetricsSnapshot {
        int totalPosts();
        int totalSubscribers();
        int totalPageViews();
        Optional<Instant> lastPostPublishedAt();
    }*/

    /*@Value.Immutable
    @JsonSerialize(as = ImmutableAudit.class)
    @JsonDeserialize(as = ImmutableAudit.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface Audit {
        Instant createdAt();
        Instant updatedAt();
        String schemaVersion();
    }*/
}
