package com.fidd.core.info.yaml;

import com.fidd.core.info.ImmutableBasicInfo;
import com.fidd.core.info.ImmutableFiddInfo;
import com.fidd.core.info.ImmutableAuthorship;
import com.fidd.core.info.ImmutableOwner;
import com.fidd.core.info.ImmutableSearchInfo;
import com.fidd.core.info.ImmutableAffiliation;
import com.fidd.core.info.ImmutableDonations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class YamlFiddInfoSerializerTest {
    @Test
    public void testSerialization() {
        YamlFiddInfoSerializer serializer = new YamlFiddInfoSerializer();
        
        Map<String, String> social = new HashMap<>();
        social.put("x", "y");
        
        ImmutableFiddInfo info = ImmutableFiddInfo.builder()
            .searchInfo(ImmutableSearchInfo.builder()
                .name("Test")
                .description("Desc")
                .tags(Arrays.asList("A", "B"))
                .build())
            .basicInfo(ImmutableBasicInfo.builder()
                .fiddCertificate("cert")
                .subtitle("sub")
                .primaryTopic("topic")
                .subTopics(Arrays.asList("t1", "t2"))
                .language("en")
                .timezone("UTC")
                .build())
            .authorship(ImmutableAuthorship.builder()
                .owner(ImmutableOwner.builder().name("owner").email("a@b.com").bio("bio").socialLinks(social).build())
                .allowGuestAuthors(false)
                .build())
            .affiliation(ImmutableAffiliation.builder()
                .affiliateDisclaimer(Arrays.asList("disc1"))
                .build())
            .donations(ImmutableDonations.builder()
                .patreonUrl("url")
                .buyMeACoffeeUrl("url2")
                .other(social)
                .build())
            .build();
            
        byte[] bytes = serializer.serialize(info);
        System.out.println(new String(bytes));
        
        assertNotNull(serializer.deserialize(bytes));
    }
}
