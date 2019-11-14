package com.getstream.sdk.chat;

import com.getstream.sdk.chat.model.Event;
import com.getstream.sdk.chat.model.Member;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.codecs.GsonConverter;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserGsonAdapterTest {

    @org.junit.jupiter.api.Test
    void decodeMemberUserTest() {
        String data = "{ \n" +
                "    \"user\":{ \n" +
                "        \"id\":\"raspy-feather-1\",\n" +
                "        \"role\":\"user\",\n" +
                "        \"created_at\":\"2019-10-18T07:24:52.479686Z\",\n" +
                "        \"updated_at\":\"2019-11-01T20:17:51.198682Z\",\n" +
                "        \"last_active\":\"2019-11-01T20:17:51.19903Z\",\n" +
                "        \"online\":false,\n" +
                "        \"image\":\"https://getstream.io/random_svg/?id=raspy-feather-1\\u0026name=Raspy+feather\",\n" +
                "        \"name\":\"Raspy feather\"\n" +
                "    },\n" +
                "    \"role\":\"member\",\n" +
                "    \"created_at\":\"2019-10-22T07:18:33.860826Z\",\n" +
                "    \"updated_at\":\"2019-10-22T07:18:34.580717Z\"\n" +
                "}";
        Member member = GsonConverter.Gson().fromJson(data, Member.class);
        assertEquals("raspy-feather-1", member.getUser().getId());
        assertEquals("user", member.getUser().getRole());
        assertEquals("https://getstream.io/random_svg/?id=raspy-feather-1\u0026name=Raspy+feather", member.getUser().getImage());
    }

    @org.junit.jupiter.api.Test
    void decodeEventUserTest() {
        String data = "{ \n" +
                "   \"cid\":\"messaging:godevs\",\n" +
                "   \"type\":\"message.new\",\n" +
                "   \"message\":{ \n" +
                "      \"id\":\"broken-waterfall-5-bf971461-9bae-4ef9-96e1-c0cc43d76f7b\",\n" +
                "      \"text\":\"so\",\n" +
                "      \"html\":\"\\u003cp\\u003eso\\u003c/p\\u003e\\n\",\n" +
                "      \"type\":\"regular\",\n" +
                "      \"user\":{ \n" +
                "         \"id\":\"broken-waterfall-5\",\n" +
                "         \"role\":\"user\",\n" +
                "         \"created_at\":\"2019-03-08T14:45:03.243237Z\",\n" +
                "         \"updated_at\":\"2019-11-13T16:25:11.985656Z\",\n" +
                "         \"last_active\":\"2019-11-13T16:25:11.977314Z\",\n" +
                "         \"online\":true,\n" +
                "         \"image\":\"https://getstream.io/random_svg/?id=broken-waterfall-5\\u0026amp;name=Broken+waterfall\",\n" +
                "         \"name\":\"Broken waterfall\",\n" +
                "         \"niceName\":\"Test Nicename\"\n" +
                "      },\n" +
                "      \"attachments\":[ \n" +
                "\n" +
                "      ],\n" +
                "      \"latest_reactions\":[ \n" +
                "\n" +
                "      ],\n" +
                "      \"own_reactions\":[ \n" +
                "\n" +
                "      ],\n" +
                "      \"reaction_counts\":null,\n" +
                "      \"reply_count\":0,\n" +
                "      \"created_at\":\"2019-11-13T16:29:38.957711Z\",\n" +
                "      \"updated_at\":\"2019-11-13T16:29:38.957711Z\",\n" +
                "      \"mentioned_users\":[ \n" +
                "\n" +
                "      ]\n" +
                "   },\n" +
                "   \"user\":{ \n" +
                "      \"id\":\"broken-waterfall-5\",\n" +
                "      \"role\":\"user\",\n" +
                "      \"created_at\":\"2019-03-08T14:45:03.243237Z\",\n" +
                "      \"updated_at\":\"2019-11-13T16:25:11.985656Z\",\n" +
                "      \"last_active\":\"2019-11-13T16:25:11.977314Z\",\n" +
                "      \"online\":true,\n" +
                "      \"image\":\"https://getstream.io/random_svg/?id=broken-waterfall-5\\u0026amp;name=Broken+waterfall\",\n" +
                "      \"name\":\"Broken waterfall\",\n" +
                "      \"niceName\":\"Test Nicename\"\n" +
                "   },\n" +
                "   \"watcher_count\":11,\n" +
                "   \"created_at\":\"2019-11-13T16:29:38.963257961Z\",\n" +
                "   \"unread_channels\":1,\n" +
                "   \"unread_count\":1,\n" +
                "   \"total_unread_count\":1\n" +
                "}";
        Event event = GsonConverter.Gson().fromJson(data, Event.class);
        assertEquals("broken-waterfall-5", event.getUser().getId());
        assertEquals("user", event.getUser().getRole());
        assertEquals("https://getstream.io/random_svg/?id=broken-waterfall-5\u0026amp;name=Broken+waterfall", event.getMessage().getUser().getImage());
        assertEquals("Broken waterfall", event.getUser().getName());
        assertEquals("https://getstream.io/random_svg/?id=broken-waterfall-5\u0026amp;name=Broken+waterfall", event.getUser().getImage());
    }

    @org.junit.jupiter.api.Test
    void encodeUserSimpleTest() {
        User user = new User();
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        user.setLastActive(new Date());
        user.setId("123");
        user.setOnline(true);
        user.setTotalUnreadCount(1);
        user.setUnreadChannels(2);
        user.setImage("image-url");

        String json = GsonConverter.Gson().toJson(user);
        assertEquals("{\"image\":\"image-url\",\"id\":\"123\"}", json);
    }

}
